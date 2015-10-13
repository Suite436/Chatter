package data.structure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Tests the functionality of the UserProfile class.
 */
public class UserProfileTest {
    
    /**
     * Tests that the constructor fails when a null id is provided.
     */
    @Test
    public void testConstructorNullId() {
        boolean thrown = false;
        
        try {
            new UserProfile(null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A null id was passed in, but no IllegalArgumentException was thrown!", thrown);
    }
    
    /**
     * Tests that the constructor fails when a null preference set is provided.
     */
    @Test
    public void testConstructorNullPreferenceSet() {
        boolean thrown = false;
        
        Map<PreferenceCategory, Set<String>> preferences = new HashMap<PreferenceCategory, Set<String>>();
        preferences.put(PreferenceCategory.MOVIES, new HashSet<String>());
        preferences.put(PreferenceCategory.BOOKS, null);
        
        try {
            new UserProfile("123", preferences);
        } catch (IllegalArgumentException | NullPointerException e) {
            thrown = true;
        }
        
        assertTrue("A null preference set was passed in, but no Exception was thrown!", thrown);
    }
    
    /**
     * Tests that the constructor fails when a null preference is provided within a preference set.
     */
    @Test
    public void testConstructorNullPreference() {
        boolean thrown = false;
        
        Map<PreferenceCategory, Set<String>> preferences = new HashMap<PreferenceCategory, Set<String>>();
        preferences.put(PreferenceCategory.MOVIES, new HashSet<String>());
        preferences.get(PreferenceCategory.MOVIES).add(null);
        
        try {
            new UserProfile("123", preferences);
        } catch (IllegalArgumentException | NullPointerException e) {
            thrown = true;
        }
        
        assertTrue("A null preference was passed in, but no Exception was thrown!", thrown);
    }
    
    /**
     * Tests that the getPreferencesForCategory() method returns the correct preferences.
     */
    @Test
    public void testGetPreferencesForCategory() {
        PreferenceCategory category = PreferenceCategory.BOOKS;
        String preferenceId = "Hidden Empire";
        String id = "123";
        
        UserProfile profile = new UserProfile(id);
        
        profile.addPreference(category, preferenceId);
        
        assertTrue("The profile did not correctly return the preference!", profile
                .getPreferencesForCategory(category).contains(preferenceId));
        
        assertEquals("The profile did not return the correct number of preferences!", 1, profile
                .getPreferencesForCategory(category).size());
    }
    
    /**
     * Tests that the getPreferencesForCategory() method returns null when not found.
     */
    @Test
    public void testGetPreferencesForCategoryNotFound() {
        PreferenceCategory category = PreferenceCategory.BOOKS;
        String preferenceId = "Hidden Empire";
        String id = "123";
        
        UserProfile profile = new UserProfile(id);
        
        profile.addPreference(category, preferenceId);
        
        PreferenceCategory missingCategory = PreferenceCategory.MOVIES;
        
        assertNull("The profile returned a non-null value for a missing category!",
                profile.getPreferencesForCategory(missingCategory));
    }
    
    /**
     * Tests that a null preference ID is not accepted.
     */
    @Test
    public void testAddPreferenceNullID() {
        PreferenceCategory category = PreferenceCategory.BOOKS;
        String id = "123";
        
        UserProfile profile = new UserProfile(id);
        
        boolean thrown = false;
        
        try {
            profile.addPreference(category, null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue(
                "A null preference ID was passed in, but no IllegalArgumentException was thrown!",
                thrown);
    }
    
    /**
     * Tests the overridden equals() method for the UserProfile class in the successful case.
     */
    @Test
    public void testEquals() {
        UserProfile user1 = new UserProfile("Seth");
        UserProfile user2 = new UserProfile("Seth");
        
        // Test Symmetry of operation
        assertTrue("The two IDs were the same, but equals() returned false!", user1.equals(user2));
        assertTrue("The two IDs were the same, but equals() returned false!", user2.equals(user1));
    }
    
    /**
     * Tests that the overridden equals() method for the UserProfile class returns false when the
     * candidate is null.
     */
    @Test
    public void testNotEqualsNull() {
        UserProfile user1 = new UserProfile("Seth");
        
        assertFalse("The candidate user was null, but equals() returned true!", user1.equals(null));
    }
    
    /**
     * Tests that the overridden equals() method for the UserProfile class returns false when the
     * user IDs differ.
     */
    @Test
    public void testNotEquals() {
        UserProfile user1 = new UserProfile("Seth");
        UserProfile user2 = new UserProfile("Charles");
        
        assertFalse("The candidate user was had a different ID, but equals() returned true!",
                user1.equals(user2));
    }
    
    /**
     * Tests that the hashCode() method returns the same integer for equivalent objects.
     */
    @Test
    public void testHashCodeForEquivalents() {
        UserProfile user1 = new UserProfile("Seth");
        UserProfile user2 = new UserProfile("Seth");
        
        // First, validate assumption that these users are actually equivalent.
        assertEquals("Two identical UserProfiles are not considered equivalent!", user1, user2);
        
        assertEquals("The two objects are equivalent, but hashCode() returned different integers!",
                user1.hashCode(), user2.hashCode());
    }
    
    /**
     * Tests that the hashCode() method returns the same integer on successive calls.
     */
    @Test
    public void testHashCodeConsistency() {
        UserProfile user1 = new UserProfile("Seth");
        
        int firstCode = user1.hashCode();
        
        for (int i = 0; i < 100; i++) {
            if (firstCode != user1.hashCode()) {
                fail("The hashCode() method does not always return the same integer on successive calls!");
                return;
            }
        }
    }
}
