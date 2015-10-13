package data.proxy;

import java.util.Collection;

import data.structure.Preference;
import data.structure.PreferenceCategory;

public interface PreferenceCorrelationGraph {
    
    /**
     * Writes a Preference to the graph.
     * 
     * @param preference
     */
    public void write(Preference preference);
    
    /**
     * Deletes a Preference from the graph.
     * 
     * @param id
     * @param category
     */
    public void delete(String id, PreferenceCategory category);
    
    /**
     * Gets the Preference from the graph.
     * 
     * @param id
     * @param category
     * @return preference
     */
    public Preference getPreference(String id, PreferenceCategory category);
    
    /**
     * Gets all Preferences that are correlated with the provided Preference.
     * 
     * @param preference
     * @return correlated preferences
     */
    public Collection<Preference> getCorrelatedPreferences(Preference preference);
}
