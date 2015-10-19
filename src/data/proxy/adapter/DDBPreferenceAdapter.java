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
     * Constructor that sets the Preference object.
     * 
     * @param preference
     * @throws IllegalArgumentException if preference is null
     */
    public DDBPreferenceAdapter(Preference preference) {
        if (preference == null) {
            throw new IllegalArgumentException("Preference cannot be null!");
        }
        this.preference = preference;
        this.dbModel = null;
    }
    
    /**
     * Constructor that sets the DynamoDB Item.
     * 
     * @param preference
     * @throws IllegalArgumentException if dbModel is null
     */
    public DDBPreferenceAdapter(Item dbModel) {
        if (dbModel == null) {
            throw new IllegalArgumentException("Item cannot be null!");
        }
        this.preference = null;
        this.dbModel = dbModel;
    }
    
    /**
     * Generates the Preference object, if the DynamoDB Item has already been provided.
     * 
     * @return preference
     */
    public Preference toObject() {
        if (this.preference != null) {
            return this.preference;
        }
        
        String dbPreferenceID = this.dbModel.getString(PREFERENCE_ID_ATTRIBUTE);
        String preferenceID = parsePreferenceIdFromDbString(dbPreferenceID);
        PreferenceCategory category = parseCategoryFromDbString(dbPreferenceID);
        
        int popularity = this.dbModel.getInt(POPULARITY_ATTRIBUTE);
        
        this.preference = new Preference(preferenceID, category, popularity);
        
        Map<String, Integer> dbCorrelations = this.dbModel.getMap(CORRELATIONS_ATTRIBUTE);
        
        if (dbCorrelations != null) {
            for (Entry<String, Integer> correlation : dbCorrelations.entrySet()) {
                String toPreferenceDbID = correlation.getKey();
                Preference toPreference = new Preference(
                        parsePreferenceIdFromDbString(toPreferenceDbID),
                        parseCategoryFromDbString(toPreferenceDbID));
                int weight = correlation.getValue();
                this.preference.addCorrelation(new PreferenceCorrelation(toPreference, weight));
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
    public static String parsePreferenceIdFromDbString(String dbString) {
        return dbString.substring(dbString.indexOf(DB_CATEGORY_ID_SEPARATOR)
                + DB_CATEGORY_ID_SEPARATOR.length());
    }
    
    /**
     * Parses the preference category from the DynamoDB ID string
     * 
     * @param dbString
     * @return preference category
     */
    public static PreferenceCategory parseCategoryFromDbString(String dbString) {
        return PreferenceCategory.valueOf(dbString.substring(0,
                dbString.indexOf(DB_CATEGORY_ID_SEPARATOR)));
    }
    
    /**
     * Generates the DynamoDB Item, if the Preference object has already been provided.
     * 
     * @return DynamoDB Item
     */
    public Item toDBModel() {
        if (this.dbModel != null) {
            return this.dbModel;
        }
        
        Map<String, Integer> dbCorrelations = new HashMap<String, Integer>();
        for (PreferenceCorrelation correlation : this.preference.getCorrelations()) {
            Preference correlatedPreference = correlation.getToPreference();
            dbCorrelations.put(
                    buildDbIdFromComponents(correlatedPreference.getID(),
                            correlatedPreference.getCategory()), correlation.getWeight());
        }
        
        this.dbModel = new Item()
                .withPrimaryKey(
                        PREFERENCE_ID_ATTRIBUTE,
                        buildDbIdFromComponents(this.preference.getID(),
                                this.preference.getCategory()))
                .withInt(POPULARITY_ATTRIBUTE, this.preference.getPopularity())
                .withMap(CORRELATIONS_ATTRIBUTE, dbCorrelations);
        
        return this.dbModel;
    }
    
    /**
     * Builds the DyanamoDB key String from a Preference object.
     * 
     * @param id
     * @param category
     * @return DB ID String
     */
    public static String buildDbIdFromComponents(String id, PreferenceCategory category) {
        return String.format("%s%s%s", category, DB_CATEGORY_ID_SEPARATOR, id);
    }
    
    /**
     * Builds a DyanamoDB attribute path.
     * 
     * @param pathComponents
     * @return attributePath
     */
    public static String buildDbAttributePath(String... pathComponents) {
        StringBuilder builder = new StringBuilder(pathComponents[0]);
        
        for (int i = 1; i < pathComponents.length; i++) {
            builder.append(".");
            builder.append(pathComponents[i]);
        }
        
        return builder.toString();
    }
}
