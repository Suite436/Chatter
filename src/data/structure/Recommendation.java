package data.structure;

/**
 * Represents a recommendation based on a UserProfile with its associated Preferences
 *    (i.e. the user's preferences).  Contains the Preference that is most correlated
 *      with the user's preferences as well as total correlation score.
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((correlatedPreference == null) ? 0 : correlatedPreference
						.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((user == null) ? 0 : user.hashCode());
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
		Recommendation other = (Recommendation) obj;
		if (correlatedPreference == null) {
			if (other.correlatedPreference != null)
				return false;
		} else if (!correlatedPreference.equals(other.correlatedPreference))
			return false;
		if (Double.doubleToLongBits(score) != Double
				.doubleToLongBits(other.score))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}
