package server.daemons;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import data.proxy.PreferenceCorrelationGraph;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.Recommendation;
import data.structure.Tuple2;
import data.structure.UserProfile;

/**
 * A service (not really a daemon) that generates a recommendation for an item that does not match one of the user's existing preferences.  It uses
 *    correlation scores to determine which preference has the highest total sum of correlations with the user's existing preferences.  It is attempting
 *    to be scalable by handling preferences in batches, given that there could be a very large number of preferences in total, even within a given category.
 *    Currently recalculates all preference scores from scratch every time it is called, but will soon implement logic to allow for simply updating correlation
 *    scores based on update events.
 * 
 * @author benjaminposerow
 *
 */
public class GenerateRecommendationDaemon {
	private int batchSize;
	
	/**
	 * 
	 * @param batchSize  The number of preferences that are processed at once in the calculation of preference correlation scores between the universe of
	 *    existing preferences in a category and user preferences.  Doing these calculations in batches allows for greater opportunities for parallelism in
	 *    the determination of the best recommendation for a user amongst a potentially very large number of preferences.  It also potentially could reduce
	 *    demand for memory, because you don't need to load all preferences to do the calculation and you could potentially serialize the correlation scores
	 *    to DDB for each batch before processing the next batch.
	 */
	public GenerateRecommendationDaemon(int batchSize) {
		this.batchSize = batchSize;
	}
	
	Map<Preference, Double> getBatchScores(UserProfile user,
			List<Preference> preferenceBatch,
			PreferenceCategory preferenceCategory) {
		Set<Preference> userPreferences = user.getPreferencesForCategory(preferenceCategory);
		
		// Find those preferences that don't already belong to the user
		Set<Preference> candidatePreferences = getNonUserPreferences(preferenceBatch, userPreferences);
		
		// Take the sum of the correlation ratios between each pair of user preferences and other candidate preferences.  Sum the
		//   correlation scores across all user preferences.  Return a map of candidate preference to the sum of the correlation scores for
		//   that preference.
		return candidatePreferences.stream().map(pref -> new Tuple2<Preference, Double>(pref, 
				userPreferences.stream()
				      .map(userPref -> getCorrelationRatio(pref, userPref))
				     .reduce(0.0, (a, b) -> a + b)))
		             .collect(Collectors.toMap(pair -> pair._1(), pair -> pair._2()));
	}

	/**
	 * Given a batch of preferences, figures out which preferences don't already belong to the user
	 * 
	 * @param preferenceBatch
	 * @param userPreferences  Preferences that are already associated with a given user
	 * @return
	 */
	private Set<Preference> getNonUserPreferences(
			List<Preference> preferenceBatch, Set<Preference> userPreferences) {
		Set<Preference> candidatePreferences = new HashSet<Preference>();
		candidatePreferences.addAll(preferenceBatch);
		
		if (userPreferences != null) {
			candidatePreferences.removeAll(userPreferences);
		}
		return candidatePreferences;
	}

	/**
	 * Gets the normalized correlation ratio between a given preference and a user preference.  Returns 0.0 if there is no correlation between
	 *    the preferences.
	 * 
	 * @param pref
	 * @param userPref
	 * @return
	 */
	private double getCorrelationRatio(Preference pref, Preference userPref) {
		PreferenceCorrelation correlation = userPref.findCorrelation(pref);
		return correlation != null ? correlation.getCorrelationRatio(userPref.getPopularity()) : 0.0;
	}
	

	/**
	 * Given a map of preferences to correlation scores, finds the entry in the map with the highest correlation score.  This entry, if it
	 *   exists, will provide the user recommendation.
	 * 
	 * @param correlationScoreMap Map of preferences to their associated correlation scores (with all user preferences).
	 * @param comparator Used to order the entries of the map by correlation score
	 * @return The map entry with the maximum correlation score or Optional.empty() if there are no preferences that are correlated with user preferences
	 */
	private Optional<Map.Entry<Preference, Double>> getMaxMapEntry(
			Map<Preference, Double> correlationScoreMap,
			Comparator<Map.Entry<Preference, Double>> comparator) {
		return correlationScoreMap.isEmpty() ? Optional.empty() : Optional.of(Collections.max(correlationScoreMap.entrySet(), comparator));
	}
	
	/**
	 * Find the best preference recommendation, based on correlation score, relative to user preferences.
	 * 
	 * @param preferenceCategory Constrains preference recommendations to the category of preferences specified, e.g. books
	 * @param user  User profile, which includes the preferences that belong to the user
	 * @param correlationGraph  Graph that allows one to access all preferences and their correlated preferences
	 * @return
	 */
	public Optional<Recommendation> getRecommendation(
			PreferenceCategory preferenceCategory,
			UserProfile user, 
			PreferenceCorrelationGraph correlationGraph) {
		
		// Returns batches of preferences from the correlation graph, with each batch matching the batch size
		Stream<List<Preference>> preferences = correlationGraph.batchGetPreferences(preferenceCategory, batchSize);
		
		// For each batch, calculate a map of preference to total correlation score
		Stream<Map<Preference, Double>> prefsToScores = preferences.map(preferenceBatch -> 
		     getBatchScores(user, preferenceBatch, preferenceCategory));
		
		// This comparator simply orders map entries by correlation score
		Comparator<Map.Entry<Preference, Double>> entryComparator = (e1, e2) -> (e1.getValue() - e2.getValue()) > 0.0 ? 1 : -1;
		
		
		Optional<Map.Entry<Preference, Double>> topScoredEntry = prefsToScores
				// For each batch find the map entry with highest correlation score
				.map(preferenceMap -> getMaxMapEntry(preferenceMap, entryComparator))
				// Filter out empty batches 
				.filter(optionalEntry -> optionalEntry.isPresent())
				// Just get the actual map entries from the Optional object
				.map(optionalEntry -> optionalEntry.get())
				// You now have the max entries of each batch, next get the max entry amongst all of the batches
				.max(entryComparator);
	    
		// Finally create a recommendation object out of the map entry with the greatest correlation score
		return topScoredEntry.map(entry -> new Recommendation(entry.getKey(), user, entry.getValue()));
	}
}
