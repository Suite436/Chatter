package server.daemons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import data.proxy.LocalTransientPreferenceCorrelationGraph;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.Recommendation;
import data.structure.UserProfile;

public class GenerateRecommendationDaemonTest {
	private final static double TOLERANCE = 0.001;
	
	private GenerateRecommendationDaemon daemon = null;
	
	@Before
	public void setUp() {
		daemon = new GenerateRecommendationDaemon(3);
	}
	
	
	/**
	 * 	Happy Day Scenario.  Setup 4 preferences for 4 different books.  Harry Potter and Ender's Game will belong to the user, while Saga of the 
     *     Seven Suns and Xenocide will be additional preferences.  Make sure correlation scores are calculated correctly and that
	 *     Xenocide, which will have a higher correlation score to the user's preferences, is chosen as the recommendation.
	 */
	@Test
	public void testSimpleRecommendationBatch() {
		Preference harryPotterPref = new Preference("Harry Potter", PreferenceCategory.BOOKS, 100);
		Preference endersGamePref = new Preference("Ender's Game", PreferenceCategory.BOOKS, 50);
		
		Preference sevenSunsPref = new Preference("Saga of the Seven Suns", PreferenceCategory.BOOKS, 15);
		Preference xenocidePref = new Preference("Xenocide", PreferenceCategory.BOOKS, 20);
		
		
		PreferenceCorrelation hpToEg = new PreferenceCorrelation(endersGamePref, 10);
		PreferenceCorrelation egToHp = new PreferenceCorrelation(harryPotterPref, 10);
		
		harryPotterPref.addCorrelation(hpToEg);
		endersGamePref.addCorrelation(egToHp);
		
		PreferenceCorrelation hpToSevenSuns = new PreferenceCorrelation(sevenSunsPref, 1);
		PreferenceCorrelation hpToXenocide = new PreferenceCorrelation(xenocidePref, 2);
		harryPotterPref.addAllCorrelations(Arrays.asList(hpToSevenSuns, hpToXenocide));
		
		PreferenceCorrelation egToSevenSuns = new PreferenceCorrelation(sevenSunsPref, 5);
		PreferenceCorrelation egToXenocide = new PreferenceCorrelation(xenocidePref, 15);
		endersGamePref.addAllCorrelations(Arrays.asList(egToSevenSuns, egToXenocide));
		
		
		PreferenceCorrelation sevenSunsToEg = new PreferenceCorrelation(endersGamePref, 5);
		PreferenceCorrelation sevenSunsToXenocide = new PreferenceCorrelation(xenocidePref, 4);
		PreferenceCorrelation sevenSunsToHP = new PreferenceCorrelation(harryPotterPref, 1);
		sevenSunsPref.addAllCorrelations(Arrays.asList(sevenSunsToEg, sevenSunsToXenocide, sevenSunsToHP));
		
		
		PreferenceCorrelation xenocideToEg = new PreferenceCorrelation(endersGamePref, 15);
		PreferenceCorrelation xenocideToSevenSuns = new PreferenceCorrelation(sevenSunsPref, 4);
		PreferenceCorrelation xenocideToHP = new PreferenceCorrelation(harryPotterPref, 2);
		xenocidePref.addAllCorrelations(Arrays.asList(xenocideToEg, xenocideToSevenSuns, xenocideToHP));
		
		
		Map<PreferenceCategory, Set<Preference>> userPreferences = ImmutableMap.of(PreferenceCategory.BOOKS, 
				ImmutableSet.of(harryPotterPref, endersGamePref));
		
		UserProfile userProfile = new UserProfile("bposerow", userPreferences);
	
		LocalTransientPreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		
		
		correlationGraph.putPreference(sevenSunsPref);
		correlationGraph.putPreference(xenocidePref);
		
		
		Map<Preference, Double> preferenceScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS);
		assertEquals(2, preferenceScores.size());
		
		assertTrue(Math.abs(0.32 - preferenceScores.get(xenocidePref)) < TOLERANCE);
		assertTrue(Math.abs(0.11 - preferenceScores.get(sevenSunsPref)) < TOLERANCE);
		
		Optional<Recommendation> recommendation = daemon.getRecommendation(PreferenceCategory.BOOKS, userProfile, correlationGraph);
		
		assertEquals(xenocidePref, recommendation.get().getCorrelatedPreference());
		assertTrue(Math.abs(0.32 - recommendation.get().getScore()) < TOLERANCE);
	}
	
	/**
	 * Test case in which the entire preference list only includes the same preferences as user preferences
	 */
	@Test
	public void testNoPreferencesOtherThanCurrentUser() {
		Preference harryPotterPref = new Preference("Harry Potter", PreferenceCategory.BOOKS, 100);
		Preference endersGamePref = new Preference("Ender's Game", PreferenceCategory.BOOKS, 50);
		
		PreferenceCorrelation hpToEg = new PreferenceCorrelation(endersGamePref, 10);
		PreferenceCorrelation egToHp = new PreferenceCorrelation(harryPotterPref, 10);
		
		harryPotterPref.addCorrelation(hpToEg);
		endersGamePref.addCorrelation(egToHp);
		
		Map<PreferenceCategory, Set<Preference>> userPreferences = ImmutableMap.of(PreferenceCategory.BOOKS, 
				ImmutableSet.of(harryPotterPref, endersGamePref));
		
		UserProfile userProfile = new UserProfile("bposerow", userPreferences);
	
		LocalTransientPreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		
		Map<Preference, Double> preferenceScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref), PreferenceCategory.BOOKS);
		assertTrue(preferenceScores.isEmpty());		
	}
	
	/**
	 * Test to make sure that a Preference without associated correlations, in this case the Saga of the Seven Suns Preference, still works as expected,
	 *    in this case the score should come out as 0
	 */
	@Test
	public void testPreferenceMissingCorrelation() {
		
		Preference harryPotterPref = new Preference("Harry Potter", PreferenceCategory.BOOKS, 100);
		Preference endersGamePref = new Preference("Ender's Game", PreferenceCategory.BOOKS, 50);
		
		Preference sevenSunsPref = new Preference("Saga of the Seven Suns", PreferenceCategory.BOOKS, 15);
		Preference xenocidePref = new Preference("Xenocide", PreferenceCategory.BOOKS, 20);
		
		
		PreferenceCorrelation hpToEg = new PreferenceCorrelation(endersGamePref, 10);
		PreferenceCorrelation egToHp = new PreferenceCorrelation(harryPotterPref, 10);
		
		harryPotterPref.addCorrelation(hpToEg);
		endersGamePref.addCorrelation(egToHp);
		
		PreferenceCorrelation hpToXenocide = new PreferenceCorrelation(xenocidePref, 2);
		harryPotterPref.addAllCorrelations(Arrays.asList(hpToXenocide));
		
		PreferenceCorrelation egToXenocide = new PreferenceCorrelation(xenocidePref, 15);
		endersGamePref.addAllCorrelations(Arrays.asList(egToXenocide));
		
		
		PreferenceCorrelation sevenSunsToEg = new PreferenceCorrelation(endersGamePref, 5);
		PreferenceCorrelation sevenSunsToXenocide = new PreferenceCorrelation(xenocidePref, 4);
		PreferenceCorrelation sevenSunsToHP = new PreferenceCorrelation(harryPotterPref, 1);
		sevenSunsPref.addAllCorrelations(Arrays.asList(sevenSunsToEg, sevenSunsToXenocide, sevenSunsToHP));
		
		
		PreferenceCorrelation xenocideToEg = new PreferenceCorrelation(endersGamePref, 15);
		PreferenceCorrelation xenocideToHP = new PreferenceCorrelation(harryPotterPref, 2);
		xenocidePref.addAllCorrelations(Arrays.asList(xenocideToEg, xenocideToHP));
		
		
		Map<PreferenceCategory, Set<Preference>> userPreferences = ImmutableMap.of(PreferenceCategory.BOOKS, 
				ImmutableSet.of(harryPotterPref, endersGamePref));
		
		UserProfile userProfile = new UserProfile("bposerow", userPreferences);
	
		LocalTransientPreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		
		
		correlationGraph.putPreference(sevenSunsPref);
		correlationGraph.putPreference(xenocidePref);
		
		
		Map<Preference, Double> preferenceScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS);
		assertEquals(1, preferenceScores.size());
		
		assertTrue(Math.abs(0.32 - preferenceScores.get(xenocidePref)) < TOLERANCE);
		//assertTrue(Math.abs(0.0 - preferenceScores.get(sevenSunsPref)) < TOLERANCE);
		
		
		Optional<Recommendation> recommendation = daemon.getRecommendation(PreferenceCategory.BOOKS, userProfile, correlationGraph);
		
		assertEquals(xenocidePref, recommendation.get().getCorrelatedPreference());
		assertTrue(Math.abs(0.32 - recommendation.get().getScore()) < TOLERANCE);
	}
	
	/**
	 * Test a special case in which there are no preferences at all, make sure logic still works (and recommends nothing)
	 */
	@Test
	public void noPreferencesNoUserPreferences() {
		Map<PreferenceCategory, Set<Preference>> userPreferences = new HashMap<PreferenceCategory, Set<Preference>>();
		UserProfile userProfile = new UserProfile("bposerow", userPreferences);
		
		Map<Preference, Double> preferenceScores = daemon.calculateCorrelationScores(userProfile, new ArrayList<Preference>(), PreferenceCategory.BOOKS);
		
		assertTrue(preferenceScores.isEmpty());
	}
	
	/**
	 * Test setting the batch size to 1 so as to force multiple batches (because there are 2 preferences in the test above that don't belong to the
	 *     user).
	 */
	@Test
	public void testMultipleBatches() {
		// Just reinitialize batch size to 1
		daemon = new GenerateRecommendationDaemon(1);
		// Perform same test as above to make sure that test still works when there are multiple batches
		testSimpleRecommendationBatch();
	}
}