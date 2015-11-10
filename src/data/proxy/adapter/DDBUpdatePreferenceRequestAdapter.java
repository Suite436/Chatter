package data.proxy.adapter;

import java.util.Map.Entry;

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;

import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

public class DDBUpdatePreferenceRequestAdapter {
    
    private UpdatePreferenceRequest request;
    private UpdateItemSpec dbModel;
    
    /**
     * Constructor requires the UpdateRequest object.
     * 
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public DDBUpdatePreferenceRequestAdapter(UpdatePreferenceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request canot be null!");
        }
        this.request = request;
        this.dbModel = null;
    }
    
    /**
     * Generates the DynamoDB UpdateItemSpec, if the UpdateRequest object has already been provided.
     * 
     * @return UpdateItemSpec
     */
    public UpdateItemSpec toDBModel() {
        if (this.dbModel != null) {
            return this.dbModel;
        }
        
        Preference preferenceToUpdate = this.request.getPreferenceToUpdate();
        
        // Create new UpdateItemSpec.
        UpdateItemSpec spec = createNewUpdateForPreference(preferenceToUpdate.getCategory(),
                preferenceToUpdate.getID());
        
        UpdateAction popularityUpdate = request.getPopularityUpdate();
        
        if (popularityUpdate != null) {
            // Update popularity.
            addAttributeUpdate(spec, DDBPreferenceAdapter.POPULARITY_ATTRIBUTE,
                    popularityUpdate.getDelta());
        }
        
        // Update correlation weights.
        for (Entry<PreferenceCorrelation, UpdateAction> update : this.request
                .getCorrelationUpdates().entrySet()) {
            PreferenceCorrelation correlationToUpdate = update.getKey();
            Preference toPreference = correlationToUpdate.getToPreference();
            String toPreferenceId = toPreference.getID();
            PreferenceCategory category = toPreference.getCategory();
            String dbPreferenceId = DDBPreferenceAdapter.buildDbIdFromComponents(toPreferenceId,
                    category);
            String dbAttributePath = DDBPreferenceAdapter.buildDbAttributePath(
                    DDBPreferenceAdapter.CORRELATIONS_ATTRIBUTE, dbPreferenceId);
            addAttributeUpdate(spec, dbAttributePath, update.getValue().getDelta());
        }
        
        this.dbModel = spec;
        
        return this.dbModel;
    }
    
    /**
     * Creates an UpdateItemSpec with a primary key associated with the provided preference
     * identifiers.
     * 
     * @param category
     * @param preferenceID
     * @return spec
     */
    private UpdateItemSpec createNewUpdateForPreference(PreferenceCategory category,
            String preferenceID) {
        return new UpdateItemSpec().withPrimaryKey(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE,
                DDBPreferenceAdapter.buildDbIdFromComponents(preferenceID, category));
    }
    
    /**
     * Adds an attribute update to the UpdateItemSpec that adjusts the value of an attribute by the
     * specified amount.
     * 
     * @param spec
     * @param attributePath
     * @param delta
     */
    private void addAttributeUpdate(UpdateItemSpec spec, String attributePath, int delta) {
        spec.addAttributeUpdate(new AttributeUpdate(attributePath).addNumeric(delta));
    }
}
