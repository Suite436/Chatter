package data.proxy.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.amazonaws.services.dynamodbv2.document.Item;

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
     * @return
     */
    public DDBUserProfileAdapter withObject(UserProfile userProfile) {
        this.userProfile = userProfile;
        
        // If the UserProfile object is reset, do not retain any previously-existing DynamoDB Item.
        this.dbModel = null;
        
        return this;
    }
    
    /**
     * Sets the DynamoDB Item.
     * 
     * @param dbModel
     * @return
     */
    public DDBUserProfileAdapter withDBModel(Item dbModel) {
        this.dbModel = dbModel;
        
        // If the DynamoDB Item is reset, do not retain any previously-existing UserProfile object.
        this.userProfile = null;
        
        return this;
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
        
        if (this.dbModel == null) {
            throw new IllegalStateException(
                    "You cannot create a UserProfile object without first providing a DBModel!");
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
        
        if (this.userProfile == null) {
            throw new IllegalStateException(
                    "You cannot create the DBModel without first providing a UserProfile object!");
        }
        
        Map<String, Set<String>> dbPreferences = new HashMap<String, Set<String>>();
        for (Entry<PreferenceCategory, Set<String>> preferenceCategory : this.userProfile
                .getPreferences().entrySet()) {
            dbPreferences.put(preferenceCategory.getKey().name(), preferenceCategory.getValue());
        }
        
        this.dbModel = new Item().withPrimaryKey(USER_ID_ATTRIBUTE, this.userProfile.getId())
                .withMap(PREFERENCE_MAP_ATTRIBUTE, dbPreferences);
        
        return this.dbModel;
    }
}
