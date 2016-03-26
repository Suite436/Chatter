package data.proxy.adapter;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.Item;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.UserProfile;

/**
 * Tests the functionality of the DDBUserProfileAdapter class.
 */
public class DDBUserProfileAdapterTest {
    
    private static final String TEST_USER_ID = "TestUser";
    private static final PreferenceCategory PREFERENCE_CATEGORY_TO_USE = PreferenceCategory.BOOKS;
    private static final String PREFERENCE_ID = "LordOfTheRings";
    
    private UserProfile testProfile;
    private Item testModel;
    
    /**
     * Creates the UserProfile object and corresponding DynamoDB model to be used in the subsequent
     * tests.
     */
    @Before
    public void setup() {
        Map<String, Set<String>> dbPreferences = new HashMap<String, Set<String>>();
        dbPreferences.put(PREFERENCE_CATEGORY_TO_USE.name(), new HashSet<String>());
        dbPreferences.get(PREFERENCE_CATEGORY_TO_USE.name()).add(PREFERENCE_ID);
        
        Map<PreferenceCategory, Set<Preference>> preferences = new HashMap<PreferenceCategory, Set<Preference>>();
        preferences.put(PREFERENCE_CATEGORY_TO_USE, new HashSet<Preference>());
        preferences.get(PREFERENCE_CATEGORY_TO_USE).add(
                new Preference(PREFERENCE_ID, PREFERENCE_CATEGORY_TO_USE));
        
        testProfile = new UserProfile(TEST_USER_ID, preferences);
        testModel = new Item()
                .withPrimaryKey(DDBUserProfileAdapter.USER_ID_ATTRIBUTE, TEST_USER_ID).withMap(
                        DDBUserProfileAdapter.PREFERENCE_MAP_ATTRIBUTE, dbPreferences);
    }
    
    /**
     * Tests that the toObject() method cannot be called without first setting the DynamoDB Item.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingModel() {
        
        new DDBUserProfileAdapter((Item) null);
    }
    
    /**
     * Tests that the toDBModel() method cannot be called without first setting the UserProfile
     * object.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMissingObject() {
        new DDBUserProfileAdapter((UserProfile) null);
    }
    
    /**
     * Tests the conversion of a UserProfile object to a DynamoDB model.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testToDBModel() {
        DDBUserProfileAdapter adapter = new DDBUserProfileAdapter(testProfile);
        Item result = adapter.toDBModel();
        
        assertEquals("The returned Item did not have the expected ID!",
                testModel.getString(DDBUserProfileAdapter.USER_ID_ATTRIBUTE),
                result.getString(DDBUserProfileAdapter.USER_ID_ATTRIBUTE));
        
        for (Entry<String, Object> entry : testModel.getMap(
                DDBUserProfileAdapter.PREFERENCE_MAP_ATTRIBUTE).entrySet()) {
            Set<String> dbPreferences = (Set<String>) result.getMap(
                    DDBUserProfileAdapter.PREFERENCE_MAP_ATTRIBUTE).get(entry.getKey());
            assertEquals("The returned Item did not have the expected preferences!",
                    entry.getValue(), dbPreferences);
        }
    }
    
    /**
     * Tests the conversion of a DynamoDB model to a UserProfile object.
     */
    @Test
    public void testToObject() {
        DDBUserProfileAdapter adapter = new DDBUserProfileAdapter(testModel);
        UserProfile result = adapter.toObject();
        
        assertEquals("The returned UserProfile did not have the expected ID!", testProfile.getId(),
                result.getId());
        
        assertEquals("The returned UserProfile did not have the expected preferences!",
                testProfile.getPreferences(), result.getPreferences());
    }
}
