package data.proxy.request;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import data.structure.Preference;
import data.structure.PreferenceCorrelation;

/**
 * UpdatePreferenceRequest provides a simple object to represent a set of updates that are to be
 * applied to a Preference.
 */
public class UpdatePreferenceRequest {
    
    private final Preference preferenceToUpdate;
    private UpdateAction popularityUpdate;
    private final Map<PreferenceCorrelation, UpdateAction> correlationUpdates;
    
    /**
     * UpdateAction represents an action that can be performed on an attribute in an update.
     */
    public enum UpdateAction {
        INC_CORRELATION(1), DEC_CORRELATION(-1);
        
        private int delta;
        
        /**
         * Constructor requires the delta associated with the update.
         * 
         * @param delta
         */
        private UpdateAction(int delta) {
            this.delta = delta;
        }
        
        /**
         * Gets the delta associated with the update.
         * 
         * @return delta
         */
        public int getDelta() {
            return this.delta;
        }
    }
    
    /**
     * Constructor requires preferenceToUpdate.
     * 
     * @param preferenceToUpdate
     * @throws IllegalArgumentException if preferenceToUpdate is null
     */
    public UpdatePreferenceRequest(Preference preferenceToUpdate) {
        if (preferenceToUpdate == null) {
            throw new IllegalArgumentException("Preference cannot be null");
        }
        this.preferenceToUpdate = preferenceToUpdate;
        this.correlationUpdates = new HashMap<PreferenceCorrelation, UpdateAction>();
    }
    
    /**
     * Gets the preference that is to be updated.
     * 
     * @return preference to update
     */
    public Preference getPreferenceToUpdate() {
        return this.preferenceToUpdate;
    }
    
    /**
     * Updates the popularity of the preference.
     * 
     * @param action
     */
    public void updatePopularity(UpdateAction action) {
        this.popularityUpdate = action;
    }
    
    /**
     * Getter for popularity update action.
     * 
     * @return popularity update action
     */
    public UpdateAction getPopularityUpdate() {
        return this.popularityUpdate;
    }
    
    /**
     * Adds an update to a correlation.
     * 
     * @param correlationId
     * @param action
     */
    public void addCorrelationUpdate(PreferenceCorrelation correlation, UpdateAction action) {
        this.correlationUpdates.put(correlation, action);
    }
    
    /**
     * Gets the updates.
     * 
     * @return updates
     */
    public Map<PreferenceCorrelation, UpdateAction> getCorrelationUpdates() {
        return Collections.unmodifiableMap(this.correlationUpdates);
    }
}
