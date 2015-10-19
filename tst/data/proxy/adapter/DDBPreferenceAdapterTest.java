package data.proxy.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.Item;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

/**
 * Tests the functionality of the DDBPreferenceAdapter class.
 */
public class DDBPreferenceAdapterTest {
    
    private static final PreferenceCategory PREFERENCE_CATEGORY_TO_USE = PreferenceCategory.BOOKS;
    private static final String PREFERENCE_ID = "LordOfTheRings";
    private static final int PREFERENCE_POPULARITY = 20;
    private static final String CORRELATION_PREFERENCE_ID = "HarryPotter";
    private static final int CORRELATION_WEIGHT = 10;
    
    private Preference testPreference;
    private PreferenceCorrelation testCorrelation;
    private Item testModel;
    
    /**
     * Creates the Preference object and corresponding DynamoDB model to be used in the subsequent
     * tests.
     */
    @Before
    public void setup() {
        testCorrelation = new PreferenceCorrelation(new Preference(CORRELATION_PREFERENCE_ID,
                PREFERENCE_CATEGORY_TO_USE), CORRELATION_WEIGHT);
        
        Set<PreferenceCorrelation> correlations = new HashSet<PreferenceCorrelation>();
        correlations.add(testCorrelation);
        
        Map<String, Integer> dbCorrelations = new HashMap<String, Integer>();
        dbCorrelations.put(DDBPreferenceAdapter.buildDbIdFromComponents(testCorrelation
                .getToPreference().getID(), testCorrelation.getToPreference().getCategory()),
                testCorrelation.getWeight());
        
        testPreference = new Preference(PREFERENCE_ID, PREFERENCE_CATEGORY_TO_USE,
                PREFERENCE_POPULARITY, correlations);
        testModel = new Item()
                .withPrimaryKey(
                        DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE,
                        DDBPreferenceAdapter.buildDbIdFromComponents(PREFERENCE_ID,
                                PREFERENCE_CATEGORY_TO_USE))
                .withMap(DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE, dbCorrelations)
                .withInt(DDBPreferenceAdapter.POPULARITY_ATTRIBUTE, PREFERENCE_POPULARITY);
    }
    
    /**
     * Tests that the toObject() method cannot be called without first setting the DynamoDB Item.
     */
    @Test
    public void testMissingModel() {
        boolean thrown = false;
        
        try {
            DDBPreferenceAdapter adapter = new DDBPreferenceAdapter((Item) null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null DynamoDB Item was provided, but no exception was thrown!", thrown);
    }
    
    /**
     * Tests that the toDBModel() method cannot be called without first setting the Preference
     * object.
     */
    @Test
    public void testMissingObject() {
        boolean thrown = false;
        
        try {
            DDBPreferenceAdapter adapter = new DDBPreferenceAdapter((Preference) null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null Preference object was provided, but no exception was thrown!", thrown);
    }
    
    /**
     * Tests the conversion of a Preference object to a DynamoDB model.
     */
    @Test
    public void testToDBModel() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter(testPreference);
        Item result = adapter.toDBModel();
        
        assertEquals("The returned Item did not have the expected ID!",
                testModel.getString(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE),
                result.getString(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE));
        
        Map<String, Object> resultCorrelations = result
                .getMap(DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE);
        
        for (Entry<String, Object> entry : testModel.getMap(
                DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE).entrySet()) {
            assertTrue("The returned Item did not have the expected correlations!",
                    resultCorrelations.containsKey(entry.getKey()));
            
            int resultWeight = (int) resultCorrelations.get(entry.getKey());
            assertEquals("The returned correlation did not have the expected weight!",
                    entry.getValue(), resultWeight);
        }
    }
    
    /**
     * Tests the conversion of a DynamoDB model to a Preference object.
     */
    @Test
    public void testToObject() {
        DDBPreferenceAdapter adapter = new DDBPreferenceAdapter(testModel);
        Preference result = adapter.toObject();
        
        assertEquals("The returned Preference did not have the expected ID!",
                testPreference.getID(), result.getID());
        
        List<PreferenceCorrelation> expectedCorrelations = new ArrayList<PreferenceCorrelation>();
        expectedCorrelations.addAll(testPreference.getCorrelations());
        List<PreferenceCorrelation> actualCorrelations = new ArrayList<PreferenceCorrelation>();
        actualCorrelations.addAll(result.getCorrelations());
        
        assertEquals("The returned Preference did not have the expected number of correlations!",
                expectedCorrelations.size(), actualCorrelations.size());
        
        Comparator<PreferenceCorrelation> comparator = new Comparator<PreferenceCorrelation>() {
            @Override
            public int compare(PreferenceCorrelation a, PreferenceCorrelation b) {
                return a.getToPreference().getID().compareTo(b.getToPreference().getID());
            }
        };
        
        Collections.sort(expectedCorrelations, comparator);
        Collections.sort(actualCorrelations, comparator);
        
        for (int i = 0; i < expectedCorrelations.size(); i++) {
            PreferenceCorrelation expected = expectedCorrelations.get(i);
            PreferenceCorrelation actual = actualCorrelations.get(i);
            assertEquals("The returned Preference did not have the correct correlations!",
                    expected.getToPreference(), actual.getToPreference());
            assertEquals(
                    "The returned Preference's correlations did not have the correct weights!",
                    expected.getWeight(), actual.getWeight());
        }
    }
}
