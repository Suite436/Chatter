package data.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

/**
 * Tests the functionality of the LocalTransientPreferenceCorrelationGraph class.
 */
public class LocalTransientPreferenceCorrelationGraphTest {
    
    PreferenceCorrelationGraph graph;
    
    /**
     * Initializes the graph
     */
    @Before
    public void before() {
        graph = new LocalTransientPreferenceCorrelationGraph();
    }
    
    /**
     * Tests null return for missing Preference id.
     */
    @Test
    public void testGetPreferenceNonExistent() {
        Preference preference = graph.getPreference("missing_preference_id",
                PreferenceCategory.MOVIES);
        
        assertNull("A missing Preference id was passed in, but null was not returned", preference);
    }
    
    /**
     * Tests the basic write/retrieve/delete functionality of the PreferenceCorrelationGraph.
     */
    @Test
    public void testStandardPreferenceStorageCycle() {
        // Create two preferences
        final Preference p1 = new Preference("1", PreferenceCategory.MOVIES);
        final Preference p2 = new Preference("2", PreferenceCategory.MOVIES);
        
        // Add a correlation from p1 to p2
        p1.addCorrelation(new PreferenceCorrelation(p2));
        
        // Write preferences to graph
        graph.putPreference(p1);
        graph.putPreference(p2);
        
        // Check returned preferences
        assertEquals("Preferences do not match!", p1,
                graph.getPreference(p1.getID(), p1.getCategory()));
        assertEquals("Preferences do not match!", p2,
                graph.getPreference(p2.getID(), p2.getCategory()));
        
        // Check correlation
        assertEquals("The correlation was not preserved!", p2,
                graph.getPreference(p1.getID(), p1.getCategory()).getCorrelations().iterator()
                        .next().getToPreference());
        
        // Remove two preferences
        graph.delete(p1.getID(), p1.getCategory());
        graph.delete(p2.getID(), p2.getCategory());
        
        // Check returned preferences
        assertNull("Preference was not deleted!", graph.getPreference(p1.getID(), p1.getCategory()));
        assertNull("Preference was not deleted!", graph.getPreference(p2.getID(), p2.getCategory()));
    }
}
