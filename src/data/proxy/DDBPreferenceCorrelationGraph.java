package data.proxy;

import static data.proxy.adapter.DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE;

import java.util.List;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import data.proxy.adapter.DDBIdempotencyManager;
import data.proxy.adapter.DDBPreferenceAdapter;
import data.proxy.adapter.DDBUpdatePreferenceRequestAdapter;
import data.proxy.request.UpdatePreferenceRequest;
import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.UserProfile;

public class DDBPreferenceCorrelationGraph implements PreferenceCorrelationGraph {
    
    private final Table preferenceTable;
    
    /**
     * Constructor requires a client and a table name, both of which are validated.
     * 
     * @param client
     * @param preferenceTable
     */
    public DDBPreferenceCorrelationGraph(DynamoDB client, String preferenceTable) {
        try {
            Table table = client.getTable(preferenceTable);
            validateTableDescription(table.describe());
            this.preferenceTable = table;
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException(
                    String.format("The DynamoDB table \'%s\' was not found."));
        }
    }
    
    /**
     * Validates the format of the table to ensure usability.
     * 
     * @param tableDesc
     */
    private void validateTableDescription(TableDescription tableDesc) {
        List<KeySchemaElement> keySchema = tableDesc.getKeySchema();
        for (KeySchemaElement element : keySchema) {
            if (PREFERENCE_ID_ATTRIBUTE.equals(element.getAttributeName())) {
                return;
            }
        }
        throw new IllegalArgumentException(String.format(
                "The table \'%s\' is not formatted correctly!", tableDesc.getTableName()));
    }
    
    /**
     * {@inheritDoc}
     */
    public void putPreference(Preference preference) {
        Item item = new DDBPreferenceAdapter(preference).toDBModel();
        this.preferenceTable.putItem(item);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePreference(UpdatePreferenceRequest request, UserProfile user,
            UpdateAction action) {
        UpdateItemSpec spec = new DDBUpdatePreferenceRequestAdapter(request).toDBModel();
        
        // Add idempotency guard.
        spec = DDBIdempotencyManager.makeUpdateIdempotent(spec, user, action);
        
        // Submit update.
        try {
            this.preferenceTable.updateItem(spec);
        } catch (ConditionalCheckFailedException e) {
            // If the conditional check fails, then that simply means that we have already performed
            // the update.
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void delete(String id, PreferenceCategory category) {
        this.preferenceTable.deleteItem(PREFERENCE_ID_ATTRIBUTE,
                DDBPreferenceAdapter.buildDbIdFromComponents(id, category));
        
    }
    
    /**
     * {@inheritDoc}
     */
    public Preference getPreference(String id, PreferenceCategory category) {
        Item item = this.preferenceTable.getItem(PREFERENCE_ID_ATTRIBUTE,
                DDBPreferenceAdapter.buildDbIdFromComponents(id, category));
        if (item == null) {
            return null;
        } else {
            return new DDBPreferenceAdapter(item).toObject();
        }
    }
}
