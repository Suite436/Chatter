package data.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class PreferenceCorrelationTest {
    
    /**
     * Tests that an IllegalArgumentException is thrown when a null ID is provided.
     */
    @Test
    public void testConstructorNullPreferenceId() {
        boolean thrown = false;
        
        try {
            new PreferenceCorrelation(null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue(
                "A null Preference ID was passed in, but no IllegalArgumentException was thrown!",
                thrown);
    }
    
    /**
     * Tests the inc() method.
     */
    @Test
    public void testIncrement() {
        final int INITIAL_WEIGHT = 10;
        PreferenceCorrelation correlation = new PreferenceCorrelation("123", INITIAL_WEIGHT);
        
        correlation.inc();
        
        assertEquals("The inc() method does not work as expected!", INITIAL_WEIGHT + 1,
                correlation.getWeight());
    }
    
    /**
     * Tests the dec() method.
     */
    @Test
    public void testDecrement() {
        final int INITIAL_WEIGHT = 10;
        PreferenceCorrelation correlation = new PreferenceCorrelation("123", INITIAL_WEIGHT);
        
        correlation.dec();
        
        assertEquals("The dec() method does not work as expected!", INITIAL_WEIGHT - 1,
                correlation.getWeight());
    }
    
    /**
     * Tests the getCorrelationRatio() method.
     */
    @Test
    public void testCorrelationRatio() {
        final int INITIAL_WEIGHT = 10;
        final int POPULARITY = 100;
        PreferenceCorrelation correlation = new PreferenceCorrelation("123", INITIAL_WEIGHT);
        
        assertEquals("The correlation ratio is not being calculated correctly!", INITIAL_WEIGHT
                * 1.0 / POPULARITY, correlation.getCorrelationRatio(POPULARITY), 0.001);
    }
    
    /**
     * Tests that the getCorrelationRatio() method returns 0 when a popularity of 0 is provided.
     */
    @Test
    public void testCorrelationRatioZeroDenominator() {
        final int INITIAL_WEIGHT = 10;
        final int POPULARITY = 0;
        PreferenceCorrelation correlation = new PreferenceCorrelation("123", INITIAL_WEIGHT);
        
        assertEquals(
                "The correlation ratio is not being calculated correctly when the popularity is 0!",
                0, correlation.getCorrelationRatio(POPULARITY), 0.001);
    }
    
    /**
     * Tests the merge() and mergeAll() methods.
     */
    @Test
    public void testMerge() {
        final int W1 = 10;
        final int W2 = 20;
        final int W3 = 0;
        PreferenceCorrelation c1 = new PreferenceCorrelation("123", W1);
        PreferenceCorrelation c2 = new PreferenceCorrelation("123", W2);
        PreferenceCorrelation c3 = new PreferenceCorrelation("123", W3);
        
        c1.mergeAll(Arrays.asList(new PreferenceCorrelation[] { c2, c3 }));
        
        assertEquals("The merge() method did not produce the desired behavior!", W1 + W2 + W3,
                c1.getWeight());
    }
    
    /**
     * Tests the overridden equals() method for the PreferenceCorrelation class in the successful
     * case.
     */
    @Test
    public void testEquals() {
        PreferenceCorrelation c1 = new PreferenceCorrelation("123", 10);
        PreferenceCorrelation c2 = new PreferenceCorrelation("123", 20);
        
        // Test Symmetry of operation
        assertTrue("The two IDs were the same, but equals() returned false!", c1.equals(c2));
        assertTrue("The two IDs were the same, but equals() returned false!", c2.equals(c1));
    }
    
    /**
     * Tests that the overridden equals() method for the PreferenceCorrelation class returns false
     * when the candidate is null.
     */
    @Test
    public void testNotEqualsNull() {
        PreferenceCorrelation c1 = new PreferenceCorrelation("123");
        
        assertFalse("The candidate was null, but equals() returned true!", c1.equals(null));
    }
    
    /**
     * Tests that the overridden equals() method for the PreferenceCorrelation class returns false
     * when the user IDs differ.
     */
    @Test
    public void testNotEquals() {
        PreferenceCorrelation c1 = new PreferenceCorrelation("123", 10);
        PreferenceCorrelation c2 = new PreferenceCorrelation("456", 20);
        
        assertFalse("The candidate was had a different ID, but equals() returned true!",
                c1.equals(c2));
    }
    
    /**
     * Tests that the hashCode() method returns the same integer for equivalent objects.
     */
    @Test
    public void testHashCodeForEquivalents() {
        PreferenceCorrelation c1 = new PreferenceCorrelation("123");
        PreferenceCorrelation c2 = new PreferenceCorrelation("123");
        
        // First, validate assumption that these users are actually equivalent.
        assertEquals("Two identical Preference Correlations are not considered equivalent!", c1, c2);
        
        assertEquals("The two objects are equivalent, but hashCode() returned different integers!",
                c1.hashCode(), c2.hashCode());
    }
    
    /**
     * Tests that the hashCode() method returns the same integer on successive calls.
     */
    @Test
    public void testHashCodeConsistency() {
        PreferenceCorrelation c1 = new PreferenceCorrelation("123");
        
        int firstCode = c1.hashCode();
        
        for (int i = 0; i < 100; i++) {
            if (firstCode != c1.hashCode()) {
                fail("The hashCode() method does not always return the same integer on successive calls!");
                return;
            }
        }
    }
}
