package data.proxy.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.dynamodbv2.document.Item;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.UserProfile;

/**
 * DDBUserProfileAdapter serves as a bi-directional adapter between a UserProfile object and its
 * corresponding DynamoDB Item.
 */
public class DDBUserProfileAdapter {
    public static final String USER_ID_ATTRIBUTE = "UserID";
    public static final String PREFERENCE_MAP_ATTRIBUTE = "Preferences";
    
    private UserProfile userProfile;
    private Item dbModel;
    
    /**
     * Sets the UserProfile object.
     * 
     * @param userProfile
     * @throws IllegalArgumentException if userProfile is null
     */
    public DDBUserProfileAdapter(UserProfile userProfile) {
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null!");
        }
        this.userProfile = userProfile;
        this.dbModel = null;
    }
    
    /**
     * Sets the DynamoDB Item.
     * 
     * @param dbModel
     * @throws IllegalArgumentException if dbModel is null
     */
    public DDBUserProfileAdapter(Item dbModel) {
        if (dbModel == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        this.userProfile = null;
        this.dbModel = dbModel;
    }
    
    /**
     * Generates the UserProfile object, if the DynamoDB Item has already been provided.
     * 
     * @return
     * @throws IllegalStateException if the DynamoDB Item is null.
     */
    @SuppressWarnings("unchecked")
    public UserProfile toObject() {
        if (this.userProfile != null) {
            return this.userProfile;
        }
        
        this.userProfile = new UserProfile(this.dbModel.getString(USER_ID_ATTRIBUTE));
        
        Map<String, Object> dbPreferences = this.dbModel.getMap(PREFERENCE_MAP_ATTRIBUTE);
        
        if (dbPreferences != null) {
            for (Entry<String, Object> preferenceCategory : dbPreferences.entrySet()) {
                PreferenceCategory category = PreferenceCategory.valueOf(preferenceCategory
                        .getKey());
                Set<String> preferences = (Set<String>) preferenceCategory.getValue();
                for (String preferenceId : preferences) {
                    this.userProfile.addPreference(category, preferenceId);
                }
            }
        }
        
        return this.userProfile;
        
    }
    
    /**
     * Generates the DynamoDB Item, if the UserProfile object has already been provided.
     * 
     * @return
     * @throws IllegalStateException if UserProfile object is null.
     */
    public Item toDBModel() {
        if (this.dbModel != null) {
            return this.dbModel;
        }
        
        // We need a composite structure of basic types for the DDB Item.
        Map<String, Set<String>> dbPreferences = new HashMap<String, Set<String>>();
        for (Entry<PreferenceCategory, Set<Preference>> preferenceCategory : this.userProfile
                .getPreferences().entrySet()) {
            // Collect preference IDs into Set of Strings.
            Set<String> preferenceIds = preferenceCategory.getValue().stream()
                    .map((Preference p) -> p.getID()).collect(Collectors.toSet());
            
            // Add set to item attribute map.
            dbPreferences.put(preferenceCategory.getKey().name(), preferenceIds);
        }
        
        this.dbModel = new Item().withPrimaryKey(USER_ID_ATTRIBUTE, this.userProfile.getId())
                .withMap(PREFERENCE_MAP_ATTRIBUTE, dbPreferences);
        
        return this.dbModel;
    }
}
