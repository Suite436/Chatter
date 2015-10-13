package data.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Preference represents a preference which has an id, a category, and correlations with other
 * preferences.
 */
public class Preference {
    private final String id;
    private final PreferenceCategory category;
    private final List<PreferenceCorrelation> correlations;
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
            List<PreferenceCorrelation> correlations) {
        if (id == null) {
            throw new IllegalArgumentException("Preference ID cannot be null!");
        }
        if (category == null) {
            throw new IllegalArgumentException("Preference Category cannot be null!");
        }
        if (popularity < 0) {
            throw new IllegalArgumentException("Popularity cannot be less than 0!");
        }
        this.id = id;
        this.category = category;
        this.popularity = popularity;
        this.correlations = new ArrayList<PreferenceCorrelation>();
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
     * Increments the popularity of this preference.
     */
    public void incPopularity() {
        this.popularity++;
    }
    
    /**
     * Decrements the popularity of this preference.
     */
    public void decPopularity() {
        this.popularity--;
    }
    
    /**
     * Adds a correlation.
     * 
     * @param correlation
     */
    public void addCorrelation(PreferenceCorrelation correlation) {
        validateCorrelation(correlation);
        this.correlations.add(correlation);
    }
    
    /**
     * Adds a list of correlations.
     * 
     * @param correlations
     */
    public void addAllCorrelations(List<PreferenceCorrelation> correlations) {
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
    
    public List<PreferenceCorrelation> getCorrelations() {
        return Collections.unmodifiableList(this.correlations);
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
}
