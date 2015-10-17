package data.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

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
     * Tests that construction fails when a null correlation is provided.
     */
    @Test
    public void testConstructorNullCorrelation() {
        boolean thrown = false;
        
        Set<PreferenceCorrelation> correlations = new HashSet<PreferenceCorrelation>();
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
        Preference correlatedPreference = new Preference("456", PreferenceCategory.BOOKS);
        PreferenceCorrelation correlation = new PreferenceCorrelation(correlatedPreference);
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
        Preference correlatedPreference1 = new Preference("456", PreferenceCategory.BOOKS);
        Preference correlatedPreference2 = new Preference("789", PreferenceCategory.BOOKS);
        Set<PreferenceCorrelation> correlations = new HashSet<PreferenceCorrelation>();
        PreferenceCorrelation correlation1 = new PreferenceCorrelation(correlatedPreference1);
        PreferenceCorrelation correlation2 = new PreferenceCorrelation(correlatedPreference2);
        
        correlations.add(correlation1);
        correlations.add(correlation2);
        
        preference.addAllCorrelations(correlations);
        
        Set<PreferenceCorrelation> result = preference.getCorrelations();
        
        assertTrue("The first correlation was not added!", result.contains(correlation1));
        
        assertTrue("The second correlation was not added!", result.contains(correlation2));
    }
    
    /**
     * Tests the removeCorrelation() method.
     */
    @Test
    public void testRemoveCorrelation() {
        Preference preference = new Preference("123", PreferenceCategory.BOOKS);
        Preference correlatedPreference1 = new Preference("456", PreferenceCategory.BOOKS);
        Preference correlatedPreference2 = new Preference("789", PreferenceCategory.BOOKS);
        Set<PreferenceCorrelation> correlations = new HashSet<PreferenceCorrelation>();
        PreferenceCorrelation correlation1 = new PreferenceCorrelation(correlatedPreference1);
        PreferenceCorrelation correlation2 = new PreferenceCorrelation(correlatedPreference2);
        
        correlations.add(correlation1);
        correlations.add(correlation2);
        
        preference.addAllCorrelations(correlations);
        preference.removeCorrelation(correlation1);
        
        Set<PreferenceCorrelation> result = preference.getCorrelations();
        
        assertFalse("The first correlation was not removed!", result.contains(correlation1));
        
        assertTrue("The second correlation was incorrectly removed!", result.contains(correlation2));
    }
    
    /**
     * Tests the overridden equals() method for the Preference class in the successful case.
     */
    @Test
    public void testEquals() {
        Preference p1 = new Preference("123", PreferenceCategory.BOOKS, 10);
        Preference p2 = new Preference("123", PreferenceCategory.BOOKS, 20);
        
        // Test Symmetry of operation
        assertTrue("The two IDs were the same, but equals() returned false!", p1.equals(p2));
        assertTrue("The two IDs were the same, but equals() returned false!", p2.equals(p1));
    }
    
    /**
     * Tests that the overridden equals() method for the Preference class returns false when the
     * candidate is null.
     */
    @Test
    public void testNotEqualsNull() {
        Preference p1 = new Preference("123", PreferenceCategory.BOOKS);
        
        assertFalse("The candidate was null, but equals() returned true!", p1.equals(null));
    }
    
    /**
     * Tests that the overridden equals() method for the Preference class returns false when the
     * user IDs differ.
     */
    @Test
    public void testNotEquals() {
        Preference p1 = new Preference("123", PreferenceCategory.BOOKS, 10);
        Preference p2 = new Preference("456", PreferenceCategory.BOOKS, 10);
        
        assertFalse("The candidate was had a different ID, but equals() returned true!",
                p1.equals(p2));
    }
    
    /**
     * Tests that the hashCode() method returns the same integer for equivalent objects.
     */
    @Test
    public void testHashCodeForEquivalents() {
        Preference p1 = new Preference("123", PreferenceCategory.BOOKS);
        Preference p2 = new Preference("123", PreferenceCategory.BOOKS);
        
        // First, validate assumption that these users are actually equivalent.
        assertEquals("Two identical Preference Correlations are not considered equivalent!", p1, p2);
        
        assertEquals("The two objects are equivalent, but hashCode() returned different integers!",
                p1.hashCode(), p2.hashCode());
    }
    
    /**
     * Tests that the hashCode() method returns the same integer on successive calls.
     */
    @Test
    public void testHashCodeConsistency() {
        Preference p1 = new Preference("123", PreferenceCategory.BOOKS);
        
        int firstCode = p1.hashCode();
        
        for (int i = 0; i < 100; i++) {
            if (firstCode != p1.hashCode()) {
                fail("The hashCode() method does not always return the same integer on successive calls!");
                return;
            }
        }
    }
}
