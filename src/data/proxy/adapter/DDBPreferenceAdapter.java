package data.proxy.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.dynamodbv2.document.Item;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

/**
 * DDBPreferenceAdapter serves as a bi-directional adapter between a Preference object and its
 * corresponding DynamoDB Item.
 */
public class DDBPreferenceAdapter {
    public static final String PREFERENCE_ID_ATTRIBUTE = "PreferenceID";
    public static final String POPULARITY_ATTRIBUTE = "Popularity";
    public static final String CORRELATIONS_ATTRIBUTE = "Correlations";
    private static final String DB_CATEGORY_ID_SEPARATOR = "~~";
    
    private Preference preference;
    private Item dbModel;
    
    /**
     * Sets the Preference object.
     * 
     * @param userProfile
     * @return
     */
    public DDBPreferenceAdapter withObject(Preference preference) {
        this.preference = preference;
        
        // If the Preference object is reset, do not retain any previously-existing DynamoDB Item.
        this.dbModel = null;
        
        return this;
    }
    
    /**
     * Sets the DynamoDB Item.
     * 
     * @param dbModel
     * @return
     */
    public DDBPreferenceAdapter withDBModel(Item dbModel) {
        this.dbModel = dbModel;
        
        // If the DynamoDB Item is reset, do not retain any previously-existing Preference object.
        this.preference = null;
        
        return this;
    }
    
    /**
     * Generates the Preference object, if the DynamoDB Item has already been provided.
     * 
     * @return
     * @throws IllegalStateException if the DynamoDB Item is null.
     */
    public Preference toObject() {
        if (this.preference != null) {
            return this.preference;
        }
        
        if (this.dbModel == null) {
            throw new IllegalStateException(
                    "You cannot create a Preference object without first providing a DBModel!");
        }
        
        String dbPreferenceID = this.dbModel.getString(PREFERENCE_ID_ATTRIBUTE);
        String preferenceID = parsePreferenceIDFromDBString(dbPreferenceID);
        PreferenceCategory category = parseCategoryFromDBString(dbPreferenceID);
        
        int popularity = this.dbModel.getInt(POPULARITY_ATTRIBUTE);
        
        this.preference = new Preference(preferenceID, category, popularity);
        
        Map<String, Integer> dbCorrelations = this.dbModel.getMap(CORRELATIONS_ATTRIBUTE);
        
        if (dbCorrelations != null) {
            for (Entry<String, Integer> correlation : dbCorrelations.entrySet()) {
                String toPreferenceID = correlation.getKey();
                int weight = correlation.getValue();
                this.preference.addCorrelation(new PreferenceCorrelation(toPreferenceID, weight));
            }
        }
        
        return this.preference;
        
    }
    
    /**
     * Parses the preference ID from the DynamoDB ID string
     * 
     * @param dbString
     * @return preference ID
     */
    public static String parsePreferenceIDFromDBString(String dbString) {
        return dbString.substring(dbString.indexOf(DB_CATEGORY_ID_SEPARATOR)
                + DB_CATEGORY_ID_SEPARATOR.length());
    }
    
    /**
     * Parses the preference category from the DynamoDB ID string
     * 
     * @param dbString
     * @return preference category
     */
    public static PreferenceCategory parseCategoryFromDBString(String dbString) {
        return PreferenceCategory.valueOf(dbString.substring(0,
                dbString.indexOf(DB_CATEGORY_ID_SEPARATOR)));
    }
    
    /**
     * Generates the DynamoDB Item, if the Preference object has already been provided.
     * 
     * @return
     * @throws IllegalStateException if Preference object is null.
     */
    public Item toDBModel() {
        if (this.dbModel != null) {
            return this.dbModel;
        }
        
        if (this.preference == null) {
            throw new IllegalStateException(
                    "You cannot create the DBModel without first providing a Preference object!");
        }
        
        Map<String, Integer> dbCorrelations = new HashMap<String, Integer>();
        for (PreferenceCorrelation correlation : this.preference.getCorrelations()) {
            dbCorrelations.put(correlation.getToPreferenceID(), correlation.getWeight());
        }
        
        this.dbModel = new Item()
                .withPrimaryKey(
                        PREFERENCE_ID_ATTRIBUTE,
                        buildDBStringFromComponents(this.preference.getID(),
                                this.preference.getCategory()))
                .withInt(POPULARITY_ATTRIBUTE, this.preference.getPopularity())
                .withMap(CORRELATIONS_ATTRIBUTE, dbCorrelations);
        
        return this.dbModel;
    }
    
    /**
     * Builds the DyanamoDB key String from a Preference object.
     * 
     * @param preference
     * @return DBString
     */
    public static String buildDBStringFromComponents(String id, PreferenceCategory category) {
        return String.format("%s%s%s", category, DB_CATEGORY_ID_SEPARATOR, id);
    }
}
