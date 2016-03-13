package server.daemons;

import data.structure.Preference;

public class WeightChange {
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