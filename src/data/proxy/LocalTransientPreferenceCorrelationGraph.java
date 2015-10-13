package data.proxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import data.proxy.adapter.DDBPreferenceAdapter;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

/**
 * LocalTransientPreferenceCorrelationGraph manages access to the stored preferences and preference
 * correlations locally in memory.
 */
public class LocalTransientPreferenceCorrelationGraph implements PreferenceCorrelationGraph {
    
    private Map<String, Preference> preferences;
    
    /**
     * Basic default constructor for LocalTransientPreferenceCorrelationGraph.
     */
    public LocalTransientPreferenceCorrelationGraph() {
        preferences = new HashMap<String, Preference>();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Preference preference) {
        this.preferences.put(
                DDBPreferenceAdapter.buildDBStringFromComponents(preference.getID(),
                        preference.getCategory()), preference);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String id, PreferenceCategory category) {
        this.preferences.remove(DDBPreferenceAdapter.buildDBStringFromComponents(id, category));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Preference getPreference(String id, PreferenceCategory category) {
        return this.preferences.get(DDBPreferenceAdapter.buildDBStringFromComponents(id, category));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Preference> getCorrelatedPreferences(Preference preference) {
        Collection<Preference> correlatedPreferences = new ArrayList<Preference>();
        
        for (PreferenceCorrelation correlation : preference.getCorrelations()) {
            correlatedPreferences.add(this.preferences.get(correlation.getToPreferenceID()));
        }
        
        return correlatedPreferences;
    }
    
}
