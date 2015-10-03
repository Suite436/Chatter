package data.structure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import data.proxy.adapter.PreferenceCategory;

/**
 * UserProfile represents a user, containing all user metadata.
 */
public class UserProfile {
    private final String id;
    private final Map<PreferenceCategory, Set<String>> preferences;
    
    /**
     * Constructor requires id, but not preferences.
     * 
     * @param id
     */
    public UserProfile(String id) {
        this(id, null);
    }
    
    /**
     * Constructor requires non-null id and can take preferences.
     * 
     * @param id
     * @param preferences
     * @throws IllegalArgumentException if id is null
     */
    public UserProfile(String id, Map<PreferenceCategory, Set<String>> preferences) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null!");
        }
        this.id = id;
        this.preferences = new HashMap<PreferenceCategory, Set<String>>();
        if (preferences != null) {
            addAllPreferences(preferences);
        }
    }
    
    /**
     * Getter for id.
     * 
     * @return id
     */
    public String getId() {
        return id;
    }
    
    /**
     * Adds a preference for the user.
     * 
     * @param category
     * @param preferenceId
     */
    public void addPreference(PreferenceCategory category, String preferenceId) {
        validatePreference(category, preferenceId);
        if (!this.preferences.containsKey(category)) {
            this.preferences.put(category, new HashSet<String>());
        }
        this.preferences.get(category).add(preferenceId);
    }
    
    /**
     * Removes the specified preference.
     * 
     * @param category
     * @param preferenceId
     */
    public void removePreference(PreferenceCategory category, String preferenceId) {
        validatePreference(category, preferenceId);
        if (this.preferences.containsKey(category)) {
            Set<String> preferences = this.preferences.get(category);
            if (preferences != null) {
                preferences.remove(preferenceId);
            }
        }
    }
    
    /**
     * Adds all supplied preferences for the user.
     * 
     * @param preferences
     */
    public void addAllPreferences(Map<PreferenceCategory, Set<String>> preferences) {
        for (Entry<PreferenceCategory, Set<String>> preferenceCategory : preferences.entrySet()) {
            PreferenceCategory category = preferenceCategory.getKey();
            for (String preference : preferenceCategory.getValue()) {
                addPreference(category, preference);
            }
        }
    }
    
    /**
     * Gets the user's preferences in the specified category, or null if none exist.
     * 
     * @param category
     * @return preferences
     */
    public Set<String> getPreferencesForCategory(PreferenceCategory category) {
        return this.preferences.get(category);
    }
    
    /**
     * Returns an unmodifiable version of the user's preferences.
     * 
     * @return unmodifiable version of the user's preferences
     */
    public Map<PreferenceCategory, Set<String>> getPreferences() {
        return Collections.unmodifiableMap(this.preferences);
    }
    
    /**
     * Override of Object.equals(), based solely on user ID.
     * 
     * @param obj candidate for equality
     * @return boolean for equality
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof UserProfile)) {
            return false;
        }
        UserProfile user = (UserProfile) obj;
        return this.id.equals(user.getId());
    }
    
    /**
     * Override of Object.hashCode(), based on user ID.
     * 
     * @return hashCode for user
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
    
    /**
     * Validates a preference.
     * 
     * @param category
     * @param preferenceId
     * @throws IllegalArgumentException if the category or preference ID is invalid.
     */
    private void validatePreference(PreferenceCategory category, String preferenceId) {
        if (category == null) {
            throw new IllegalArgumentException("The category cannot be null!");
        }
        if (preferenceId == null) {
            throw new IllegalArgumentException("A preference cannot have a null ID!");
        }
    }
    
}
