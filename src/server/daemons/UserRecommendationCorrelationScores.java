package server.daemons;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import util.Tuple2;
import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.UserProfile;

/**
 * Encapsulates total correlation scores between each given Preference and all user
 *    preferences for a given user.  Used to generate recommendations for that user.
 *    Allows updates to these correlation scores based on UpdatePreferenceRequest(s).  
 *    Provides operations to aggregate these correlation scores to aid in determining
 *    which Preference should be recommended to a given user.
 */
public class UserRecommendationCorrelationScores {
	private final Map<Preference, Double> correlationScores;
	private final UserProfile user;
	
	/**
	 * 
	 * @param correlationScores Mapping from a non-user preference to the total correlation
	 *      score, i.e.
	 *      sum(weight/popularity) over all user preferences
	 * @param user Contains all user preferences and basic user information
	 */
	public UserRecommendationCorrelationScores(Map<Preference, Double> correlationScores,
			UserProfile user) {
		this.correlationScores = correlationScores;
		this.user = user;
	}
	
	/**
	 * Update correlation scores based on UpdatePreferenceRequest
	 * 
	 * Note, there are a few implicit assumptions here 
	 * a) The Preference and PreferenceCorrelation objects have already had their weights 
	 *     and popularities updated as the result
	 *    of the effect of the UpdatePreferenceRequest before this method is called.  So
	 *    the code here uses the weights and correlations associated with these objects as the
	 *    "new" weights and popularities and then uses the UpdatePreferenceRequest to derive
	 *    the previous values of the popularities and weights. 
	 *  b) I am also assuming that the correlation scores are being updated synchronously directly after
	 *    the weights and popularities are updated. 
	 *  c) There is no support here for reentrancy so I assume these events will only be applied once.
	 *  These assumptions were made for simplicity for now.  If any of these assumptions are wrong, 
	 *   this logic will break, but in that case, we will need to start to attach previous popularities
	 *    and weights to UpdatePreferenceRequest(s) and/or we will need to keep track of which events
	 *    we have already applied, perhaps through versioning or logging.
	 *    
	 * 
	 * @param update  UpdatePreferenceRequest which represents a popularity change
	 *             in a given preference and its associated correlation/weight changes
	 * @return This object, modified as a result of the correlation score change
	 */
	public UserRecommendationCorrelationScores applyPreferenceUpdate(
			UpdatePreferenceRequest update) {
		Preference targetPref = update.getPreferenceToUpdate();
		PreferenceCategory preferenceCategory = targetPref.getCategory();
		
		if (targetPref == null || preferenceCategory == null) {
			throw new RuntimeException("Dude, your request is invalid");
		}
		
		// For each correlation update, match up preference to UpdateAction
		Map<Preference, UpdateAction> correlationUpdates = update.getCorrelationUpdates().entrySet().stream()
			       .collect(Collectors.toMap(entry -> entry.getKey().getToPreference(), 
			    		   entry -> entry.getValue()));
			
		Set<PreferenceCorrelation> allCorrelations = targetPref.getCorrelations();
		
		if (allCorrelations == null) {
			throw new RuntimeException("Dude, you need correlations configured here");
		}
		
		// We need to process all correlations that are currently associated with the
		//   target preference, i.e the preference with a popularity update
		for (PreferenceCorrelation prefCorrelation : allCorrelations) {
			Preference correlatedPreference = prefCorrelation.getToPreference();
		
			// Assume that weight change is already applied to correlation and use the
			//   current value as the new value
			double newWeight = prefCorrelation.getWeight();
			
			// Back into the original weight by effectively undoing the UpdateAction
			double originalWeight = getOriginalCorrelationWeight(correlationUpdates,
					correlatedPreference, newWeight);
			
			// Group together various fields that are useful in the succeeding calculations
			WeightChange weightChange = new WeightChange(targetPref, correlatedPreference,
					originalWeight, newWeight);
			
			// User preference popularity updates need to be handled differently from updates
			//   to preferences that don't belong to the user.  User preference popularity
			//   updates result in changes to denominator (popularity) and potentially the numerator (weight)
			//    of each individual
			//   correlation score and have to be applied to all non-user preferences associated
			//   with that user preference.  Non user preferences popularity updates will only
			//   affect the correlation score of that preference and can only potentially affect the
			//   numerator (weight) of the correlation score
			if (isUserPreference(targetPref, preferenceCategory)) {
				updateCorrelationScoresBasedOnUserPreferenceUpdate(
						(double) update.getPopularityUpdate().getDelta(), weightChange);

			} else if (isUserPreference(correlatedPreference, preferenceCategory) &&
					correlationUpdates.containsKey(correlatedPreference)) {
				updateCorrelationScoresBasedOnCorrelatedPreferenceUpdate(
						weightChange);
		   }
		}
		
		return this;
	}
	
	/**
	 * 
	 * @param popularityDelta  Change in popularity associated with user preference in UpdatePreferenceRequest
	 *     which is being used to change correlation scores
	 * @param weightChange Represents parameters associated with a change in weight with one
	 *          correlation of a given preference
	 */
	private void updateCorrelationScoresBasedOnUserPreferenceUpdate(
			double popularityDelta, WeightChange weightChange) {
		Preference targetPref = weightChange.getSourcePreference();
		Preference correlatedPref = weightChange.getDestinationPreference();
		PreferenceCategory preferenceCategory = targetPref.getCategory();
		double newPopularity = targetPref.getPopularity();
		double originalWeight = weightChange.getOriginalWeight();
		double newWeight = weightChange.getNewWeight();
		
		// Effectively "undo" the result of applying the UpdatePreferenceRequest to the 
		//   popularity of the Preference object
		double originalPopularity = newPopularity - popularityDelta;
		
		// We are only interested in changes in weights in preferences that don't already
		//   belong to this user
		if (! isUserPreference(correlatedPref, preferenceCategory)) {
			// Adjust current correlation score of this correlated preference by taking into
			//   account both the weight change of this correlated preference as well as the 
			//   popularity change of the associated user preference
			correlationScores.compute(correlatedPref, 
				(key, value) ->
		             getNewCorrelationScore(value, originalWeight, newWeight, originalPopularity, newPopularity));
		}
	}
	
	/**
	 * Simply returns true if the targetPref is one of the user's current preferences, false otherwise
	 * 
	 * @param targetPref
	 * @param preferenceCategory
	 * @return
	 */
	private boolean isUserPreference(Preference targetPref, PreferenceCategory preferenceCategory) {
		return user.getPreferencesForCategory(preferenceCategory).contains(targetPref);
	}
	
	/**
	 * Backs into original correlation score by undoing effect of correlation update
	 *    contained within a UpdatePreferenceRequest.
	 * 
	 * @param correlationUpdates Matches a correlated Preference to the associated UpdateAction,
	 *     basically a flattening of the UpdatePreferenceRequest
	 * @param correlatedPreference 
	 * @param newWeight
	 * @return
	 */
	private double getOriginalCorrelationWeight(
			Map<Preference, UpdateAction> correlationUpdates,
			Preference correlatedPreference, double newWeight) {
		return correlationUpdates.containsKey(correlatedPreference) ?
				newWeight - correlationUpdates.get(correlatedPreference).getDelta() :
					newWeight;
	}
	
	/**
	 * Update correlation score of a given non-user preference by applying the weight change
	 *   that it had with a particular user preference.
	 * 
	 * @param weightChange
	 */
	private void updateCorrelationScoresBasedOnCorrelatedPreferenceUpdate(
			WeightChange weightChange) {
		Preference correlatedPreference = weightChange.getDestinationPreference();
		double originalPopularity = correlatedPreference.getPopularity();
		
		correlationScores.compute(weightChange.getSourcePreference(), 
				(key, value) -> getNewCorrelationScore(value, weightChange.getOriginalWeight(), 
						weightChange.getNewWeight(), originalPopularity, originalPopularity));
	}
	
	/**
	 * Adjusts current correlation score of a given preference by taking one weight
	 * change and one associated optional popularity change and adjust the total correlation 
	 *  score accordingly.  
	 * 
	 * @param originalScore  Correlation score before this update was applied
	 * @param originalWeight  Original weight of non-user correlated preference
	 * @param newWeight  New weight of non-user correlated preference
	 * @param originalPopularity  Original popularity of user preference
	 * @param newPopularity  New popularity of user preference, might be the same as original
	 *              popularity if the user preference popularity did not change
	 * @return
	 */
	private double getNewCorrelationScore(Double originalScore, double originalWeight, 
			double newWeight, double originalPopularity, double newPopularity) {
		return (originalScore == null) ? newWeight / newPopularity :
			(newWeight/newPopularity - originalWeight/originalPopularity) + originalScore;
	}
	
	/**
	 * Given a map of preferences to correlation scores, finds the entry in the map with the highest correlation score.  This entry, if it
	 *   exists, will provide the user recommendation.
	 * 
	 * @param correlationScoreMap Map of preferences to their associated correlation scores (with all user preferences).
	 * @param comparator Used to order the entries of the map by correlation score
	 * @return The map entry with the maximum correlation score or Optional.empty() if there are no preferences that are correlated with user preferences
	 */
	public Optional<Tuple2<Preference, Double>> getMaxRecommendedPreferenceAndCorrelation() {
		if (correlationScores.isEmpty()) {
			 return Optional.empty();
		}
		
		// This comparator simply orders map entries by correlation score
		Comparator<Map.Entry<Preference, Double>> entryComparator = (e1, e2) -> (e1.getValue() - e2.getValue()) > 0.0 ? 1 : -1;		
		
		Entry<Preference, Double> maxEntry = Collections.max(correlationScores.entrySet(), entryComparator);
		return Optional.of(new Tuple2<>(maxEntry.getKey(), maxEntry.getValue()));
	}

	/**
	 * 
	 * @return  Underlying scores by Preference
	 */
	public final Map<Preference, Double> getCorrelationScores() {
		return new HashMap<Preference, Double>(correlationScores);
	}
}


