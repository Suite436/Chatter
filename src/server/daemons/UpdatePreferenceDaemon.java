package server.daemons;

import java.util.Map.Entry;
import java.util.Set;

import data.proxy.PreferenceCorrelationGraph;
import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.UserProfile;

/**
 * UpdatePreferenceDaemon propagates preference additions and removals for a given user through the
 * preference correlation graph.
 */
public class UpdatePreferenceDaemon {
    
    private final PreferenceCorrelationGraph graph;
    
    /**
     * Constructor requires a PreferenceCorrelationGraph.
     * 
     * @param graph
     * @throws IllegalArgumentException if graph is null
     */
    public UpdatePreferenceDaemon(PreferenceCorrelationGraph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph cannot be null!");
        }
        this.graph = graph;
    }
    
    /**
     * Propagates a preference addition by a user through the preference correlation graph.
     * 
     * @param user
     * @param addedPreference
     */
    public void propagateAddedPreference(UserProfile user, Preference addedPreference) {
        adjustUserPreference(user, addedPreference, UpdateAction.INC_CORRELATION);
    }
    
    /**
     * Propagates a preference removal by a user through the preference correlation graph.
     * 
     * @param user
     * @param removedPreference
     */
    public void propagateRemovedPreference(UserProfile user, Preference removedPreference) {
        adjustUserPreference(user, removedPreference, UpdateAction.DEC_CORRELATION);
    }
    
    /**
     * Adjust preference popularitiy and correlation weight in accordance with preference addition
     * or removal.
     * 
     * @param changedPreference
     * @param user
     * @param action
     */
    private void adjustUserPreference(UserProfile user, Preference changedPreference,
            UpdateAction action) {
        // First, adjust all correlations from the changed preference to all other preferences
        // possessed by the instigating user.
        adjustPreferencePopularity(changedPreference, user, action);
        
        // Then, adjust all correlations from all other preferences possessed by the instigating
        // user to the changed preference.
        adjustReverseCorrelations(user, changedPreference, action);
    }
    
    /**
     * Updates a changed preference's popularity and the weights of all correlations from the
     * changed preference to the other preferences of the user which added or removed it. This
     * action is idempotent.
     * 
     * @param changedPreference
     * @param user
     * @param action
     */
    private void adjustPreferencePopularity(Preference changedPreference, UserProfile user,
            UpdateAction action) {
        // Create new UpdatePreferenceRequest
        UpdatePreferenceRequest request = new UpdatePreferenceRequest(changedPreference);
        
        // Adjust popularity
        request.updatePopularity(action);
        
        // Adjust weights of existing correlations.
        for (Entry<PreferenceCategory, Set<Preference>> entry : user.getPreferences().entrySet()) {
            PreferenceCategory category = entry.getKey();
            for (Preference preference : entry.getValue()) {
                String preferenceId = preference.getID();
                if (category == changedPreference.getCategory()
                        && !preferenceId.equals(changedPreference.getID())) {
                    // Create PreferenceCorrelation for update.
                    PreferenceCorrelation correlationToUpdate = new PreferenceCorrelation(
                            preference);
                    // Update correlation weight.
                    request.addCorrelationUpdate(correlationToUpdate, action);
                }
            }
        }
        
        // Submit update
        this.graph.updatePreference(request, user, action);
    }
    
    /**
     * Adjusts the weights of all correlations from each preference of the instigating user to the
     * newly added or removed preference. This action is idempotent.
     * 
     * @param user
     * @param changedPreference
     * @param action
     */
    private void adjustReverseCorrelations(UserProfile user, Preference changedPreference,
            UpdateAction action) {
        // Iterate over user preferences.
        for (Entry<PreferenceCategory, Set<Preference>> entry : user.getPreferences().entrySet()) {
            PreferenceCategory category = entry.getKey();
            for (Preference preferenceToUpdate : entry.getValue()) {
                String preferenceToUpdateId = preferenceToUpdate.getID();
                // Create new UpdatePreferenceRequest.
                UpdatePreferenceRequest request = new UpdatePreferenceRequest(new Preference(
                        preferenceToUpdateId, category));
                
                // Adjust weights of (or add, if new) correlations.
                // The category must match, because we currently do not correlate across
                // categories. Also ignore the altered preference, because there should be no
                // reflexive edges.
                if (category == changedPreference.getCategory()
                        && !preferenceToUpdateId.equals(changedPreference.getID())) {
                    // Create PreferenceCorrelation for update.
                    PreferenceCorrelation correlationToUpdate = new PreferenceCorrelation(
                            changedPreference);
                    request.addCorrelationUpdate(correlationToUpdate, action);
                }
                
                // Perform update.
                this.graph.updatePreference(request, user, action);
            }
        }
    }
}
