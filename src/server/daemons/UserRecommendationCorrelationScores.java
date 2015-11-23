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

public class UserRecommendationCorrelationScores {
	private final Map<Preference, Double> correlationScores;
	private final UserProfile user;
	
	public UserRecommendationCorrelationScores(Map<Preference, Double> correlationScores,
			UserProfile user) {
		this.correlationScores = correlationScores;
		this.user = user;
	}
	
	public UserRecommendationCorrelationScores applyPreferenceUpdate(UpdatePreferenceRequest update) {
		Preference targetPref = update.getPreferenceToUpdate();
		PreferenceCategory preferenceCategory = targetPref.getCategory();
		
		// For each correlation update, match up preference to UpdateAction
		Map<Preference, UpdateAction> correlationUpdates = update.getCorrelationUpdates().entrySet().stream()
			       .collect(Collectors.toMap(entry -> entry.getKey().getToPreference(), 
			    		   entry -> entry.getValue()));
			
		Set<PreferenceCorrelation> allCorrelations = update.getPreferenceToUpdate().getCorrelations();
		
		for (PreferenceCorrelation prefCorrelation : allCorrelations) {
			Preference correlatedPreference = prefCorrelation.getToPreference();
		
			double newWeight = prefCorrelation.getWeight();
			
			double originalWeight = getOriginalCorrelationWeight(correlationUpdates,
					correlatedPreference, newWeight);
			
			WeightChange weightChange = new WeightChange(targetPref, correlatedPreference,
					originalWeight, newWeight);
			
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
	
	private void updateCorrelationScoresBasedOnUserPreferenceUpdate(
			double popularityDelta, WeightChange weightChange) {
		Preference targetPref = weightChange.getSourcePreference();
		Preference correlatedPref = weightChange.getDestinationPreference();
		PreferenceCategory preferenceCategory = targetPref.getCategory();
		double newPopularity = targetPref.getPopularity();
		double originalWeight = weightChange.getOriginalWeight();
		double newWeight = weightChange.getNewWeight();
		
		double originalPopularity = newPopularity - popularityDelta /* update.getPopularityUpdate().getDelta() */;
		
		if (! isUserPreference(correlatedPref, preferenceCategory)) {
			correlationScores.compute(correlatedPref, 
				(key, value) ->
		             getNewCorrelationScore(value, originalWeight, newWeight, originalPopularity, newPopularity));
		}
	}
	
	private boolean isUserPreference(Preference targetPref, PreferenceCategory preferenceCategory) {
		return user.getPreferencesForCategory(preferenceCategory).contains(targetPref);
	}
	
	private double getOriginalCorrelationWeight(
			Map<Preference, UpdateAction> correlationUpdates,
			Preference correlatedPreference, double newWeight) {
		return correlationUpdates.containsKey(correlatedPreference) ?
				newWeight - correlationUpdates.get(correlatedPreference).getDelta() :
					newWeight;
	}
	
	private void updateCorrelationScoresBasedOnCorrelatedPreferenceUpdate(
			WeightChange weightChange) {
		Preference correlatedPreference = weightChange.getDestinationPreference();
		double originalPopularity = correlatedPreference.getPopularity();
		
		correlationScores.compute(weightChange.getSourcePreference(), 
				(key, value) -> getNewCorrelationScore(value, weightChange.getOriginalWeight(), 
						weightChange.getNewWeight(), originalPopularity, originalPopularity));
	}
	
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
	
	private static class WeightChange {
		private final Preference sourcePreference;
		private final Preference destinationPreference;
		private final double originalWeight;
		private final double newWeight;
		
		public WeightChange(Preference sourcePreference,
				Preference destinationPreference, double originalWeight,
				double newWeight) {
			super();
			this.sourcePreference = sourcePreference;
			this.destinationPreference = destinationPreference;
			this.originalWeight = originalWeight;
			this.newWeight = newWeight;
		}
		
		public Preference getSourcePreference() {
			return sourcePreference;
		}
		public Preference getDestinationPreference() {
			return destinationPreference;
		}
		public double getOriginalWeight() {
			return originalWeight;
		}
		public double getNewWeight() {
			return newWeight;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime
					* result
					+ ((destinationPreference == null) ? 0
							: destinationPreference.hashCode());
			long temp;
			temp = Double.doubleToLongBits(newWeight);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(originalWeight);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime
					* result
					+ ((sourcePreference == null) ? 0 : sourcePreference
							.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			WeightChange other = (WeightChange) obj;
			
			if (destinationPreference == null) {
				if (other.destinationPreference != null)
					return false;
			} else if (!destinationPreference
					.equals(other.destinationPreference))
				return false;
			if (Double.doubleToLongBits(newWeight) != Double
					.doubleToLongBits(other.newWeight))
				return false;
			if (Double.doubleToLongBits(originalWeight) != Double
					.doubleToLongBits(other.originalWeight))
				return false;
			if (sourcePreference == null) {
				if (other.sourcePreference != null)
					return false;
			} else if (!sourcePreference.equals(other.sourcePreference))
				return false;
			return true;
		}
	}

	public final Map<Preference, Double> getCorrelationScores() {
		return new HashMap<Preference, Double>(correlationScores);
	}
}


