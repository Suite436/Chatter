package data.structure;

import java.util.Collection;

/**
 * PreferenceCorrelation represents a directed correlation edge with a Preference.
 */
public class PreferenceCorrelation {
    private final Preference toPreference;
    private int weight;
    
    /**
     * Constructor requires the preference of the destination, but not weight.
     * 
     * @param toPreference
     * @throws IllegalArgumentException if toPreference is null;
     */
    public PreferenceCorrelation(Preference toPreference) {
        this(toPreference, 1);
    }
    
    /**
     * Constructor requires the preference of the destination and can take weight.
     * 
     * @param toPreference
     * @param weight
     * @throws IllegalArgumentException if toPreferenceID is null;
     */
    public PreferenceCorrelation(Preference toPreference, int weight) {
        if (toPreference == null) {
            throw new IllegalArgumentException("Destination preference cannot be null!");
        }
        this.toPreference = toPreference;
        this.weight = weight;
    }
    
    /**
     * Getter for the destination preference.
     * 
     * @return toPreference
     */
    public Preference getToPreference() {
        return this.toPreference;
    }
    
    /**
     * Sets the weight of this edge.
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    /**
     * Getter for weight.
     * 
     * @return weight
     */
    public int getWeight() {
        return this.weight;
    }
    
    /**
     * Returns the correlation ratio for this edge.
     * 
     * @param popularityOfPreference
     * @return correlation ratio
     */
    public double getCorrelationRatio(int popularityOfPreference) {
        return popularityOfPreference == 0 ? 0 : this.weight * 1.0 / popularityOfPreference;
    }
    
    /**
     * Merges two PreferenceCorrelation objects together, combining their weights.
     * 
     * @param other
     * @throws IllegalArgumentException if the destination preferences do not match
     */
    public PreferenceCorrelation merge(PreferenceCorrelation other) {
        if (!getToPreference().equals(other.getToPreference())) {
            throw new IllegalArgumentException(
                    "You can only merge PreferenceCorrelations that point to the same preference!");
        }
        
        this.weight += other.getWeight();
        
        return this;
    }
    
    /**
     * Merges many PreferenceCorrelation objects together, combining their weights.
     * 
     * @param others
     * @throws IllegalArgumentException if any of the destination preferences do not match
     */
    public PreferenceCorrelation mergeAll(Collection<PreferenceCorrelation> others) {
        return others.stream().reduce(this, (a, b) -> a.merge(b));
    }
    
    /**
     * Override of Object.equals(), based solely on the destination preference ID.
     * 
     * @param obj candidate for equality
     * @return boolean for equality
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PreferenceCorrelation)) {
            return false;
        }
        PreferenceCorrelation correlation = (PreferenceCorrelation) obj;
        return this.toPreference.equals(correlation.getToPreference());
    }
    
    /**
     * Override of Object.hashCode(), based on destination preference.
     * 
     * @return hashCode for correlation
     */
    @Override
    public int hashCode() {
        return this.toPreference.hashCode();
    }
    
    /**
     * Override of Object.toString().
     */
    @Override
    public String toString() {
        return String.format("To: {Category: %s  ID: %s} Weight: %d", this.toPreference
                .getCategory().name(), this.toPreference.getID(), this.weight);
    }
}
