package data.proxy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.UserProfile;

/**
 * LocalTransientPreferenceCorrelationGraph manages access to the stored preferences and preference
 * correlations locally in memory.
 */
public class LocalTransientPreferenceCorrelationGraph implements PreferenceCorrelationGraph {
    
	private Map<PreferenceCategory, Map<String, Preference>> preferences;
    
    /**
     * Basic default constructor for LocalTransientPreferenceCorrelationGraph.
     */
    public LocalTransientPreferenceCorrelationGraph() {
        preferences = new HashMap<PreferenceCategory, Map<String, Preference>>();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Preference getPreference(String id, PreferenceCategory category) {
        if (this.preferences.containsKey(category)) {
            return this.preferences.get(category).get(id);
        } else {
            return null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id, PreferenceCategory category) {
        if (this.preferences.containsKey(category)) {
            this.preferences.get(category).remove(id);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePreference(UpdatePreferenceRequest request, UserProfile user,
            UpdateAction action) {
        // Get preference from request.
        Preference preferenceFromRequest = request.getPreferenceToUpdate();
        PreferenceCategory category = preferenceFromRequest.getCategory();
        
        Preference preferenceToUpdate;
        // Get corresponding preference from preference graph, if it exists.
        if (this.preferences.containsKey(category)
                && this.preferences.get(category).containsKey(preferenceFromRequest.getID())) {
            preferenceToUpdate = this.preferences.get(category).get(preferenceFromRequest.getID());
        } else {
            // Create new Preference for graph.
            preferenceToUpdate = new Preference(preferenceFromRequest.getID(), category, 0);
        }
        
        // Update popularity.
        if (request.getPopularityUpdate() != null) {
            preferenceToUpdate.adjustPopularity(request.getPopularityUpdate().getDelta());
        }
        
        // Build initial Map of correlations for hash-based merge.
        Map<PreferenceCorrelation, PreferenceCorrelation> correlationsToMerge = new HashMap<PreferenceCorrelation, PreferenceCorrelation>();
        for (Entry<PreferenceCorrelation, UpdateAction> update : request.getCorrelationUpdates()
                .entrySet()) {
            PreferenceCorrelation correlationToUpdate = update.getKey();
            correlationToUpdate.setWeight(update.getValue().getDelta());
            correlationsToMerge.put(correlationToUpdate, correlationToUpdate);
        }
        
        // Merge all correlations.
        for (PreferenceCorrelation existingCorrelation : preferenceToUpdate.getCorrelations()) {
            if (correlationsToMerge.containsKey(existingCorrelation)) {
                correlationsToMerge.get(existingCorrelation).merge(existingCorrelation);
            } else {
                correlationsToMerge.put(existingCorrelation, existingCorrelation);
            }
        }
        
        // Overwrite preference's correlations
        preferenceToUpdate.addAllCorrelations(correlationsToMerge.values());
        
        // Write preference back to Map.
        putPreference(preferenceToUpdate);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void putPreference(Preference preference) {
        // Check for pre-existing category.
        if (!this.preferences.containsKey(preference.getCategory())) {
            this.preferences.put(preference.getCategory(), new HashMap<String, Preference>());
        }
        this.preferences.get(preference.getCategory()).put(preference.getID(), preference);
    }
    
    /**
     * Override of Object.toString().
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        
        for (Entry<PreferenceCategory, Map<String, Preference>> catEntry : this.preferences
                .entrySet()) {
            for (Entry<String, Preference> prefEntry : catEntry.getValue().entrySet()) {
                Preference preference = prefEntry.getValue();
                out.append(String.format("%s\r\n", preference.toString()));
            }
        }
        
        return out.toString();
    }

	@Override
	public Stream<List<Preference>> batchGetPreferences(
			PreferenceCategory category, int batchSize) {
		Iterator<List<Preference>> preferenceBatches = Iterators.partition(preferences.get(category).values().iterator(), batchSize);
		Iterable<List<Preference>> iterable = () -> preferenceBatches;
		return StreamSupport.stream(iterable.spliterator(), false);
	}
    
}
