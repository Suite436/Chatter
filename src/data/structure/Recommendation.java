package data.structure;

/**
 * Represents a recommendation based on a UserProfile with its associated Preferences
 *    (i.e. the user's preferences).  Contains the Preference that is most correlated
 *      with the user's preferences as well as total correlation score.
 * @author benjaminposerow
 *
 */
public class Recommendation {
	private Preference correlatedPreference;
	private UserProfile user;
	private double score;
	
	public Recommendation(Preference correlatedPreference,
			UserProfile user, double score) {
		this.correlatedPreference = correlatedPreference;
		this.user = user;
		this.score = score;
	}
	
	public Preference getCorrelatedPreference() {
		return correlatedPreference;
	}

	public UserProfile getUser() {
		return user;
	}
	
	public double getScore() {
		return score;
	}
}
