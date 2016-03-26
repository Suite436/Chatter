package server.daemons;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import util.StreamUtils;
import util.Tuple2;
import data.proxy.PreferenceCorrelationGraph;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.Recommendation;
import data.structure.UserProfile;

/**
 * A service (not really a daemon) that generates a recommendation for an item that does not match
 * one of the user's existing preferences. It uses correlation scores to determine which preference
 * has the highest total sum of correlations with the user's existing preferences. It is attempting
 * to be scalable by handling preferences in batches, given that there could be a very large number
 * of preferences in total, even within a given category. Currently recalculates all preference
 * scores from scratch every time it is called, but will soon implement logic to allow for simply
 * updating correlation scores based on update events.
 * 
 * @author benjaminposerow
 *
 */
public class GenerateRecommendationDaemon {
    private int batchSize;
    
    /**
     * 
     * @param batchSize The number of preferences that are processed at once in the calculation of
     *        preference correlation scores between the universe of existing preferences in a
     *        category and user preferences. Doing these calculations in batches allows for greater
     *        opportunities for parallelism in the determination of the best recommendation for a
     *        user amongst a potentially very large number of preferences. It also potentially could
     *        reduce demand for memory, because you don't need to load all preferences to do the
     *        calculation and you could potentially serialize the correlation scores to DDB for each
     *        batch before processing the next batch.
     */
    public GenerateRecommendationDaemon(int batchSize) {
        this.batchSize = batchSize;
    }
    
    UserRecommendationCorrelationScores calculateCorrelationScores(UserProfile user,
            List<Preference> preferenceBatch, PreferenceCategory preferenceCategory) {
        Set<Preference> userPreferences = user.getPreferencesForCategory(preferenceCategory);
        
        // Find those preferences that don't already belong to the user
        Set<Preference> candidatePreferences = getNonUserPreferences(preferenceBatch,
                userPreferences);
        
        // Take the sum of the correlation ratios between each pair of user preferences and other
        // candidate preferences. Sum the
        // correlation scores across all user preferences. Return a map of candidate preference to
        // the sum of the correlation scores for
        // that preference.
        return new UserRecommendationCorrelationScores(calculateCorrelationScores(userPreferences,
                candidatePreferences), user);
    }
    
    private Map<Preference, Double> calculateCorrelationScores(Set<Preference> userPreferences,
            Set<Preference> candidatePreferences) {
        return candidatePreferences
                .stream()
                .map(pref -> new Tuple2<Preference, Double>(pref, userPreferences.stream()
                        .map(userPref -> getCorrelationRatio(pref, userPref))
                        .reduce(0.0, (a, b) -> a + b))).filter(tuple -> tuple._2() > 0)
                .collect(Collectors.toMap(pair -> pair._1(), pair -> pair._2()));
    }
    
    /**
     * Given a batch of preferences, figures out which preferences don't already belong to the user
     * 
     * @param preferenceBatch
     * @param userPreferences Preferences that are already associated with a given user
     * @return
     */
    private Set<Preference> getNonUserPreferences(List<Preference> preferenceBatch,
            Set<Preference> userPreferences) {
        Set<Preference> candidatePreferences = new HashSet<Preference>();
        candidatePreferences.addAll(preferenceBatch);
        
        if (userPreferences != null) {
            candidatePreferences.removeAll(userPreferences);
        }
        return candidatePreferences;
    }
    
    /**
     * Gets the normalized correlation ratio between a given preference and a user preference.
     * Returns 0.0 if there is no correlation between the preferences.
     * 
     * @param pref
     * @param userPref
     * @return
     */
    private double getCorrelationRatio(Preference pref, Preference userPref) {
        PreferenceCorrelation correlation = userPref.findCorrelation(pref);
        return correlation != null ? correlation.getCorrelationRatio(userPref.getPopularity())
                : 0.0;
    }
    
    /**
     * Find the best preference recommendation, based on correlation score, relative to user
     * preferences.
     * 
     * @param preferenceCategory Constrains preference recommendations to the category of
     *        preferences specified, e.g. books
     * @param user User profile, which includes the preferences that belong to the user
     * @param correlationGraph Graph that allows one to access all preferences and their correlated
     *        preferences
     * @return
     */
    public Optional<Recommendation> getRecommendation(PreferenceCategory preferenceCategory,
            UserProfile user, PreferenceCorrelationGraph correlationGraph) {
        
        // Returns batches of preferences from the correlation graph, with each batch matching the
        // batch size
        Stream<List<Preference>> preferences = StreamUtils.asStream(correlationGraph
                .batchGetPreferences(preferenceCategory, batchSize));
        
        // For each batch, calculate a map of preference to total correlation score
        Stream<UserRecommendationCorrelationScores> prefsToScores = preferences
                .map(preferenceBatch -> calculateCorrelationScores(user, preferenceBatch,
                        preferenceCategory));
        
        // This comparator simply orders tuples by correlation score
        Comparator<Tuple2<Preference, Double>> entryComparator = (e1, e2) -> (e1._2() - e2._2()) > 0.0 ? 1
                : -1;
        
        Optional<Tuple2<Preference, Double>> topScoredEntry = prefsToScores
                // For each batch find the map entry with highest correlation score
                .map(correlationScores -> correlationScores
                        .getMaxRecommendedPreferenceAndCorrelation())
                // Filter out empty batches
                .filter(optionalEntry -> optionalEntry.isPresent())
                // Just get the actual map entries from the Optional object
                .map(optionalEntry -> optionalEntry.get())
                // You now have the max entries of each batch, next get the max entry amongst all of
                // the batches
                .max(entryComparator);
        
        // Finally create a recommendation object out of the map entry with the greatest correlation
        // score
        return topScoredEntry.map(tuple -> new Recommendation(tuple._1(), user, tuple._2()));
    }
}