package data.structure;

import java.util.Collection;

/**
 * PreferenceCorrelation represents a directed correlation edge with a Preference.
 */
public class PreferenceCorrelation {
    private final String toPreferenceID;
    private int weight;
    
    /**
     * Constructor requires the preference ID of the destination, but not weight.
     * 
     * @param toPreferenceID
     * @throws IllegalArgumentException if toPreferenceID is null;
     */
    public PreferenceCorrelation(String toPreferenceID) {
        this(toPreferenceID, 0);
    }
    
    /**
     * Constructor requires the preference ID of the destination and can take weight.
     * 
     * @param toPreferenceID
     * @param weight
     * @throws IllegalArgumentException if toPreferenceID is null;
     */
    public PreferenceCorrelation(String toPreferenceID, int weight) {
        if (toPreferenceID == null) {
            throw new IllegalArgumentException("Destination preference ID cannot be null!");
        }
        this.toPreferenceID = toPreferenceID;
        this.weight = weight;
    }
    
    /**
     * Getter for the destination preference ID
     * 
     * @return toPreferenceID
     */
    public String getToPreferenceID() {
        return this.toPreferenceID;
    }
    
    /**
     * Marginally increases the weight of this edge.
     */
    public void inc() {
        this.weight++;
    }
    
    /**
     * Marginally decreases the weight of this edge.
     */
    public void dec() {
        this.weight--;
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
        if (!getToPreferenceID().equals(other.getToPreferenceID())) {
            throw new IllegalArgumentException(
                    "You can only merge PreferenceCorrelations that point to the same preference!");
        }
        
        this.weight += other.getWeight();
        
        return this;
    }
    
    /**
     * Merges two PreferenceCorrelation objects together, combining their weights.
     * 
     * @param other
     * @throws IllegalArgumentException if the destination preferences do not match
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
        return this.toPreferenceID.equals(correlation.getToPreferenceID());
    }
    
    /**
     * Override of Object.hashCode(), based on destination preference ID.
     * 
     * @return hashCode for correlation
     */
    @Override
    public int hashCode() {
        return this.toPreferenceID.hashCode();
    }
}
