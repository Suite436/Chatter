package data.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Preference represents a preference which has an id, a category, and correlations with other
 * preferences.
 */
public class Preference {
    private final String id;
    private final PreferenceCategory category;
    private final Set<PreferenceCorrelation> correlations;
    private int popularity;
    
    /**
     * Constructor requires id and category, but not popularity or correlations.
     * 
     * @param id
     * @param category
     */
    public Preference(String id, PreferenceCategory category) {
        this(id, category, 1);
    }
    
    /**
     * Constructor requires id, category, and popularity, but not correlations.
     * 
     * @param id
     * @param category
     * @param popularity
     */
    public Preference(String id, PreferenceCategory category, int popularity) {
        this(id, category, popularity, null);
    }
    
    /**
     * Constructor requires id, category, and popularity, and can take correlations.
     * 
     * @param id
     * @param category
     * @param popularity
     * @param correlations
     * @throws IllegalArgumentException if id or category are null
     */
    public Preference(String id, PreferenceCategory category, int popularity,
            Set<PreferenceCorrelation> correlations) {
        if (id == null) {
            throw new IllegalArgumentException("Preference ID cannot be null!");
        }
        if (category == null) {
            throw new IllegalArgumentException("Preference Category cannot be null!");
        }
        this.id = id.trim().replaceAll("\\s", "");
        this.category = category;
        this.popularity = popularity;
        this.correlations = new HashSet<PreferenceCorrelation>();
        if (correlations != null) {
            addAllCorrelations(correlations);
        }
    }
    
    /**
     * Getter for ID.
     * 
     * @return id
     */
    public String getID() {
        return this.id;
    }
    
    /**
     * Getter for category.
     * 
     * @return category
     */
    public PreferenceCategory getCategory() {
        return this.category;
    }
    
    /**
     * Getter for popularity.
     * 
     * @return popularity
     */
    public int getPopularity() {
        return this.popularity;
    }
    
    /**
     * Adjusts the popularity of this preference.
     * 
     * @param delta
     */
    public void adjustPopularity(int delta) {
        this.popularity += delta;
    }
    
    /**
     * Adds a correlation.
     * 
     * @param correlation
     */
    public void addCorrelation(PreferenceCorrelation correlation) {
        validateCorrelation(correlation);
        
        // If we get a correlation update, we want to overwrite.
        if (this.correlations.contains(correlation)) {
            this.correlations.remove(correlation);
        }
        
        this.correlations.add(correlation);
    }
    
    /**
     * Adds a list of correlations.
     * 
     * @param correlations
     */
    public void addAllCorrelations(Collection<PreferenceCorrelation> correlations) {
        for (PreferenceCorrelation correlation : correlations) {
            addCorrelation(correlation);
        }
    }
    
    /**
     * Removes a correlation.
     * 
     * @param correlation
     */
    public void removeCorrelation(PreferenceCorrelation correlation) {
        this.correlations.remove(correlation);
    }
    
    /**
     * Returns all correlated preferences.
     * 
     * @return all correlated preferences
     */
    public Set<PreferenceCorrelation> getCorrelations() {
        return Collections.unmodifiableSet(this.correlations);
    }
    
    public PreferenceCorrelation findCorrelation(Preference toPreference) {
    	for (PreferenceCorrelation correlation : this.correlations) {
    		if (correlation.getToPreference().equals(toPreference)) {
    			return correlation;
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Validates a correlation.
     * 
     * @param correlation
     */
    private void validateCorrelation(PreferenceCorrelation correlation) {
        if (correlation == null) {
            throw new IllegalArgumentException("Correlation cannot be null!");
        }
    }
    
    /**
     * Override of Object.equals(), based solely on the preference ID and category.
     * 
     * @param obj candidate for equality
     * @return boolean for equality
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Preference)) {
            return false;
        }
        Preference preference = (Preference) obj;
        return getID().equals(preference.getID()) && getCategory() == preference.getCategory();
    }
    
    /**
     * Override of Object.hashCode(), based on preference ID and category.
     * 
     * @return hashCode for preference
     */
    @Override
    public int hashCode() {
        return (getID() + getCategory().name()).hashCode();
    }
    
    /**
     * Override of Object.toString().
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        
        // Print Category and ID.
        out.append(String.format("Category: %s  ID: %s  Popularity: %d\r\n", this.category.name(),
                this.id, this.popularity));
        
        // Print correlations.
        for (PreferenceCorrelation correlation : this.correlations) {
            out.append(String.format("\t%s\r\n", correlation.toString()));
        }
        
        return out.toString();
    }
}
