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
import data.proxy.PreferenceCorrelationGraph;
import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;
import data.structure.Recommendation;
import data.structure.UserProfile;

public class GenerateRecommendationDaemonTest {
	private final static double TOLERANCE = 0.0001;
	
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
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS).getCorrelationScores();
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
				Arrays.asList(harryPotterPref, endersGamePref), PreferenceCategory.BOOKS).getCorrelationScores();
		assertTrue(preferenceScores.isEmpty());		
	}
	
	/**
	 * Test to make sure that a Preference without associated correlations, in this case the Saga of the Seven Suns Preference, still works as expected,
	 *    in this case there should be no correlation score for this preference
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
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS).getCorrelationScores();
		assertEquals(1, preferenceScores.size());
		
		assertTrue(Math.abs(0.32 - preferenceScores.get(xenocidePref)) < TOLERANCE);
		
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
		
		Map<Preference, Double> preferenceScores = daemon.calculateCorrelationScores(userProfile, new ArrayList<Preference>(), 
				PreferenceCategory.BOOKS).getCorrelationScores();
		
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
	
	@Test 
	public void testOneUserPrefUpdate() {
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
	    
		PreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		correlationGraph.putPreference(sevenSunsPref);
		correlationGraph.putPreference(xenocidePref);
		
		UserRecommendationCorrelationScores originalCorrelationScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS);

		UpdatePreferenceRequest req = new UpdatePreferenceRequest(harryPotterPref);
		req.updatePopularity(UpdateAction.INC_CORRELATION);
		req.addCorrelationUpdate(hpToXenocide, UpdateAction.INC_CORRELATION);
		
		Map<Preference, Double> newCorrelationScores = originalCorrelationScores
				.applyPreferenceUpdate(req).getCorrelationScores();
		
		assertEquals(2, newCorrelationScores.size());
		
		assertTrue(newCorrelationScores.containsKey(sevenSunsPref));
		assertTrue(newCorrelationScores.containsKey(xenocidePref));
		
		assertTrue(Math.abs(0.11 + (1.0/100.0 - 1.0/ 99.0) - newCorrelationScores.get(sevenSunsPref)) < TOLERANCE);
		assertTrue(Math.abs(0.32 + (2.0/100.0 - 1.0/99.0) - newCorrelationScores.get(xenocidePref)) < TOLERANCE);
		
	}
	
	
	@Test 
	public void testOneNonUserPrefUpdate() {
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
	    
		PreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		correlationGraph.putPreference(sevenSunsPref);
		correlationGraph.putPreference(xenocidePref);
		
		UserRecommendationCorrelationScores correlationScores = daemon.calculateCorrelationScores(
				userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS);

		UpdatePreferenceRequest req = new UpdatePreferenceRequest(sevenSunsPref);
		req.updatePopularity(UpdateAction.INC_CORRELATION);
		req.addCorrelationUpdate(sevenSunsToEg, UpdateAction.INC_CORRELATION);
		
		Map<Preference, Double> newCorrelationScores = correlationScores
				.applyPreferenceUpdate(req).getCorrelationScores();
		
		assertEquals(2, newCorrelationScores.size());
		
		assertTrue(newCorrelationScores.containsKey(sevenSunsPref));
		assertTrue(newCorrelationScores.containsKey(xenocidePref));
		
		assertTrue(Math.abs(0.11 + (5.0/50.0 - 4.0/50.0) - newCorrelationScores.get(sevenSunsPref)) < TOLERANCE);
		assertTrue(Math.abs(0.32- newCorrelationScores.get(xenocidePref)) < TOLERANCE);		
	}
	
	
	@Test 
	public void testOneNonUserMutiCorrelationUpdate() {
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
	    
		PreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		correlationGraph.putPreference(sevenSunsPref);
		correlationGraph.putPreference(xenocidePref);
		
		UserRecommendationCorrelationScores correlationScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, sevenSunsPref, xenocidePref), PreferenceCategory.BOOKS);

		UpdatePreferenceRequest req = new UpdatePreferenceRequest(sevenSunsPref);
		req.updatePopularity(UpdateAction.INC_CORRELATION);
		req.addCorrelationUpdate(sevenSunsToEg, UpdateAction.INC_CORRELATION);
		req.addCorrelationUpdate(sevenSunsToHP, UpdateAction.DEC_CORRELATION);
		
		
		Map<Preference, Double> newCorrelationScores = correlationScores
				.applyPreferenceUpdate(req).getCorrelationScores();
		
		assertEquals(2, newCorrelationScores.size());
		
		assertTrue(newCorrelationScores.containsKey(sevenSunsPref));
		assertTrue(newCorrelationScores.containsKey(xenocidePref));
		
		assertTrue(Math.abs(0.11 + (5.0/50.0 - 4.0/50.0) + (1.0/100.0 - 2.0/100.0) - newCorrelationScores.get(sevenSunsPref)) < TOLERANCE);
		assertTrue(Math.abs(0.32 - newCorrelationScores.get(xenocidePref)) < TOLERANCE);		
	}
	
	
	@Test
	public void testPreferenceUpdateForOriginallyMissingNonUserCorrelation() {
		
		Preference harryPotterPref = new Preference("Harry Potter", PreferenceCategory.BOOKS, 100);
		Preference endersGamePref = new Preference("Ender's Game", PreferenceCategory.BOOKS, 50);
		
		Preference xenocidePref = new Preference("Xenocide", PreferenceCategory.BOOKS, 20);
		
		
		PreferenceCorrelation hpToEg = new PreferenceCorrelation(endersGamePref, 10);
		PreferenceCorrelation egToHp = new PreferenceCorrelation(harryPotterPref, 10);
		
		harryPotterPref.addCorrelation(hpToEg);
		endersGamePref.addCorrelation(egToHp);
		
		PreferenceCorrelation hpToXenocide = new PreferenceCorrelation(xenocidePref, 2);
		harryPotterPref.addAllCorrelations(Arrays.asList(hpToXenocide));
		
		PreferenceCorrelation egToXenocide = new PreferenceCorrelation(xenocidePref, 15);
		endersGamePref.addAllCorrelations(Arrays.asList(egToXenocide));
		
		
		PreferenceCorrelation xenocideToEg = new PreferenceCorrelation(endersGamePref, 15);
		PreferenceCorrelation xenocideToHP = new PreferenceCorrelation(harryPotterPref, 2);
		xenocidePref.addAllCorrelations(Arrays.asList(xenocideToEg, xenocideToHP));
		
		
		Map<PreferenceCategory, Set<Preference>> userPreferences = ImmutableMap.of(PreferenceCategory.BOOKS, 
				ImmutableSet.of(harryPotterPref, endersGamePref));
		
		UserProfile userProfile = new UserProfile("bposerow", userPreferences);
	
		LocalTransientPreferenceCorrelationGraph correlationGraph = new LocalTransientPreferenceCorrelationGraph();
		correlationGraph.putPreference(harryPotterPref);
		correlationGraph.putPreference(endersGamePref);
		
		
		correlationGraph.putPreference(xenocidePref);
		
		
		UserRecommendationCorrelationScores correlationScores = daemon.calculateCorrelationScores(userProfile, 
				Arrays.asList(harryPotterPref, endersGamePref, xenocidePref), PreferenceCategory.BOOKS);
	
		Preference sevenSunsPref = new Preference("Saga of the Seven Suns", PreferenceCategory.BOOKS, 15);
		
		UpdatePreferenceRequest req = new UpdatePreferenceRequest(harryPotterPref);
		req.updatePopularity(UpdateAction.INC_CORRELATION);
		PreferenceCorrelation hpToSevenSuns = new PreferenceCorrelation(sevenSunsPref, 1);
		harryPotterPref.addAllCorrelations(Arrays.asList(hpToSevenSuns));
		
		req.addCorrelationUpdate(hpToSevenSuns, UpdateAction.INC_CORRELATION);
		
		
		Map<Preference, Double> newCorrelationScores = correlationScores.applyPreferenceUpdate(req).getCorrelationScores();
		
		assertTrue(Math.abs(0.32 + (2/100.0 - 2/99.0) - newCorrelationScores.get(xenocidePref)) < TOLERANCE);
		assertTrue(Math.abs((1.0/100.0) - newCorrelationScores.get(sevenSunsPref)) < TOLERANCE);
		
		
		Optional<Recommendation> recommendation = daemon.getRecommendation(PreferenceCategory.BOOKS, userProfile, correlationGraph);
		
		assertEquals(xenocidePref, recommendation.get().getCorrelatedPreference());
	}
}

