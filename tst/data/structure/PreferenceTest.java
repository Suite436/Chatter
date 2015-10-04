package data.structure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PreferenceTest {
    
    /**
     * Tests that construction fails when a null ID is provided.
     */
    @Test
    public void testConstructorNullID() {
        boolean thrown = false;
        
        try {
            new Preference(null, PreferenceCategory.BOOKS);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null ID was passed in, but no IllegalArgumentException was thrown!", thrown);
    }
    
    /**
     * Tests that construction fails when a null category is provided.
     */
    @Test
    public void testConstructorNullCategory() {
        boolean thrown = false;
        
        try {
            new Preference("123", null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null category was passed in, but no IllegalArgumentException was thrown!",
                thrown);
    }
    
    /**
     * Tests that construction fails when a negative popularity is provided.
     */
    @Test
    public void testConstructorNegativePopularity() {
        boolean thrown = false;
        
        try {
            new Preference("123", PreferenceCategory.BOOKS, -1);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue(
                "A negative popularity was passed in, but no IllegalArgumentException was thrown!",
                thrown);
    }
    
    /**
     * Tests that construction fails when a null correlation is provided.
     */
    @Test
    public void testConstructorNullCorrelation() {
        boolean thrown = false;
        
        List<PreferenceCorrelation> correlations = new ArrayList<PreferenceCorrelation>();
        correlations.add(null);
        
        try {
            new Preference("123", PreferenceCategory.BOOKS, 1, correlations);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null correlation was passed in, but no IllegalArgumentException was thrown!",
                thrown);
    }
    
    /**
     * Tests the addCorrelation() method.
     */
    @Test
    public void testAddCorrelation() {
        Preference preference = new Preference("123", PreferenceCategory.BOOKS);
        PreferenceCorrelation correlation = new PreferenceCorrelation("456");
        preference.addCorrelation(correlation);
        
        assertTrue("The correlation was not added!",
                preference.getCorrelations().contains(correlation));
    }
    
    /**
     * Tests the addAllCorrelations() method.
     */
    @Test
    public void testAddAllCorrelations() {
        Preference preference = new Preference("123", PreferenceCategory.BOOKS);
        List<PreferenceCorrelation> correlations = new ArrayList<PreferenceCorrelation>();
        PreferenceCorrelation correlation1 = new PreferenceCorrelation("456");
        PreferenceCorrelation correlation2 = new PreferenceCorrelation("789");
        
        correlations.add(correlation1);
        correlations.add(correlation2);
        
        preference.addAllCorrelations(correlations);
        
        List<PreferenceCorrelation> result = preference.getCorrelations();
        
        assertTrue("The first correlation was not added!", result.contains(correlation1));
        
        assertTrue("The second correlation was not added!", result.contains(correlation2));
    }
    
    /**
     * Tests the removeCorrelation() method.
     */
    @Test
    public void testRemoveCorrelation() {
        Preference preference = new Preference("123", PreferenceCategory.BOOKS);
        List<PreferenceCorrelation> correlations = new ArrayList<PreferenceCorrelation>();
        PreferenceCorrelation correlation1 = new PreferenceCorrelation("456");
        PreferenceCorrelation correlation2 = new PreferenceCorrelation("789");
        
        correlations.add(correlation1);
        correlations.add(correlation2);
        
        preference.addAllCorrelations(correlations);
        preference.removeCorrelation(correlation1);
        
        List<PreferenceCorrelation> result = preference.getCorrelations();
        
        assertFalse("The first correlation was not removed!", result.contains(correlation1));
        
        assertTrue("The second correlation was incorrectly removed!", result.contains(correlation2));
    }
}
