package data.proxy;

import java.util.List;
import java.util.stream.Stream;

import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.UserProfile;

public interface PreferenceCorrelationGraph {
    
    /**
     * Gets the Preference from the graph.
     * 
     * @param id
     * @param category
     * @return preference
     */
    public Preference getPreference(String id, PreferenceCategory category);
 
    public Stream<List<Preference>> batchGetPreferences(PreferenceCategory category, int batchSize);
    
    /**
     * Writes a Preference to the graph.
     * 
     * @param preference
     */
    public void putPreference(Preference preference);
    
    /**
     * Updates a preference in the graph in an idempotent way.
     * 
     * @param request
     * @param user
     * @param action
     */
    public void updatePreference(UpdatePreferenceRequest request, UserProfile user,
            UpdateAction action);
    
    /**
     * Deletes a Preference from the graph.
     * 
     * @param id
     * @param category
     */
    public void delete(String id, PreferenceCategory category);
}
