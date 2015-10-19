package data.proxy.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.KeyAttribute;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;

import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

public class DDBUpdatePreferenceRequestAdapterTest {
    
    private static final PreferenceCategory PREFERENCE_CATEGORY_TO_USE = PreferenceCategory.BOOKS;
    private static final String PREFERENCE_ID = "LordOfTheRings";
    private static final String CORRELATION_PREFERENCE_ID_1 = "HarryPotter";
    private static final String CORRELATION_PREFERENCE_ID_2 = "TheLittleEngineThatCould";
    
    private UpdatePreferenceRequest testRequest;
    private UpdateItemSpec testModel;
    
    /**
     * Creates the UpdatePreferenceRequest object and corresponding DynamoDB UpdateItemSpec to be
     * used in the subsequent tests.
     */
    @Before
    public void setup() {
        UpdateAction actionToUse = UpdateAction.INC_CORRELATION;
        Preference preferenceToUpdate = new Preference(PREFERENCE_ID, PREFERENCE_CATEGORY_TO_USE);
        Preference correlatedPreferenceToInc1 = new Preference(CORRELATION_PREFERENCE_ID_1,
                PREFERENCE_CATEGORY_TO_USE);
        Preference correlatedPreferenceToInc2 = new Preference(CORRELATION_PREFERENCE_ID_2,
                PREFERENCE_CATEGORY_TO_USE);
        PreferenceCorrelation correlationToInc1 = new PreferenceCorrelation(
                correlatedPreferenceToInc1);
        PreferenceCorrelation correlationToInc2 = new PreferenceCorrelation(
                correlatedPreferenceToInc2);
        
        testRequest = new UpdatePreferenceRequest(preferenceToUpdate);
        testRequest.updatePopularity(actionToUse);
        testRequest.addCorrelationUpdate(correlationToInc1, actionToUse);
        testRequest.addCorrelationUpdate(correlationToInc2, actionToUse);
        
        testModel = new UpdateItemSpec();
        String dbPreferenceId = DDBPreferenceAdapter.buildDbIdFromComponents(
                preferenceToUpdate.getID(), preferenceToUpdate.getCategory());
        testModel.withPrimaryKey(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE, dbPreferenceId);
        testModel.addAttributeUpdate(new AttributeUpdate(DDBPreferenceAdapter.POPULARITY_ATTRIBUTE)
                .addNumeric(actionToUse.getDelta()));
        
        String dbCorrelationId1 = DDBPreferenceAdapter.buildDbIdFromComponents(
                correlatedPreferenceToInc1.getID(), correlatedPreferenceToInc1.getCategory());
        String correlationToUpdatePath1 = DDBPreferenceAdapter.buildDbAttributePath(
                DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE, dbCorrelationId1);
        testModel.addAttributeUpdate(new AttributeUpdate(correlationToUpdatePath1)
                .addNumeric(actionToUse.getDelta()));
        
        String dbCorrelationId2 = DDBPreferenceAdapter.buildDbIdFromComponents(
                correlatedPreferenceToInc2.getID(), correlatedPreferenceToInc2.getCategory());
        String correlationToUpdatePath2 = DDBPreferenceAdapter.buildDbAttributePath(
                DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE, dbCorrelationId2);
        testModel.addAttributeUpdate(new AttributeUpdate(correlationToUpdatePath2)
                .addNumeric(actionToUse.getDelta()));
    }
    
    /**
     * Tests that the toDBModel() method cannot be called without first setting the Preference
     * object.
     */
    @Test
    public void testMissingObject() {
        boolean thrown = false;
        
        try {
            DDBUpdatePreferenceRequestAdapter adapter = new DDBUpdatePreferenceRequestAdapter(
                    (UpdatePreferenceRequest) null);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue(
                "A null UpdatePreferenceRequest object was provided, but no exception was thrown!",
                thrown);
    }
    
    /**
     * Tests the conversion of a UpdatePreferenceRequest object to a DynamoDB model.
     */
    @Test
    public void testToDBModel() {
        DDBUpdatePreferenceRequestAdapter adapter = new DDBUpdatePreferenceRequestAdapter(
                testRequest);
        UpdateItemSpec result = adapter.toDBModel();
        
        // Test key.
        for (KeyAttribute key1 : testModel.getKeyComponents()) {
            boolean found = false;
            for (KeyAttribute key2 : result.getKeyComponents()) {
                if (key2.getName().equals(key1.getName())
                        && key2.getValue().equals(key1.getValue())) {
                    found = true;
                }
            }
            assertTrue("The returned UpdateItemSpec did not have the correct primary key!", found);
        }
        assertEquals("The returned UpdateItemSpec had the wrong number of primary key attributes!",
                testModel.getKeyComponents().size(), result.getKeyComponents().size());
        
        // Test attribute updates.
        List<AttributeUpdate> expectedUpdates = testModel.getAttributeUpdate();
        for (AttributeUpdate expected : expectedUpdates) {
            boolean found = false;
            for (AttributeUpdate actual : result.getAttributeUpdate()) {
                if (!found
                        && expected.getAttributeName().equals(
                                DDBPreferenceAdapter.POPULARITY_ATTRIBUTE)
                        && actual.getAttributeName().equals(
                                DDBPreferenceAdapter.POPULARITY_ATTRIBUTE)) {
                    assertEquals(
                            "The returned UpdateItemSpec did not have the correct popularity update action!",
                            expected.getAction(), actual.getAction());
                    assertEquals(
                            "The returned UpdateItemSpec did not have the correct popularity update value!",
                            expected.getValue(), actual.getValue());
                    found = true;
                } else if (!found && expected.getAttributeName().equals(actual.getAttributeName())) {
                    assertEquals(
                            "The returned UpdateItemSpec did not have the correct correlation update action!",
                            expected.getAction(), actual.getAction());
                    assertEquals(
                            "The returned UpdateItemSpec did not have the correct correlation update value!",
                            expected.getValue(), actual.getValue());
                    found = true;
                }
            }
            assertTrue(String.format(
                    "The returned UpdateItemSpec did not contain an expected update! %s",
                    expected.getAttributeName()), found);
        }
        assertEquals("The returned UpdateItemSpec had the wrong number of updates!",
                expectedUpdates.size(), result.getAttributeUpdate().size());
    }
}
