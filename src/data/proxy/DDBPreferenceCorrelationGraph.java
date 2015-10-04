package data.proxy;

import static data.proxy.adapter.DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.document.BatchGetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableKeysAndAttributes;
import com.amazonaws.services.dynamodbv2.document.spec.BatchGetItemSpec;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import data.proxy.adapter.DDBPreferenceAdapter;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.PreferenceCorrelation;

public class DDBPreferenceCorrelationGraph implements PreferenceCorrelationGraph {
    
    private final DynamoDB client;
    private final Table preferenceTable;
    
    /**
     * Constructor requires a client and a table name, both of which are validated.
     * 
     * @param client
     * @param preferenceTable
     */
    public DDBPreferenceCorrelationGraph(DynamoDB client, String preferenceTable) {
        try {
            this.client = client;
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
    public void write(Preference preference) {
        Item item = new DDBPreferenceAdapter().withObject(preference).toDBModel();
        this.preferenceTable.putItem(item);
    }
    
    /**
     * {@inheritDoc}
     */
    public void delete(String id, PreferenceCategory category) {
        this.preferenceTable.deleteItem(PREFERENCE_ID_ATTRIBUTE,
                DDBPreferenceAdapter.buildDBStringFromComponents(id, category));
        
    }
    
    /**
     * {@inheritDoc}
     */
    public Preference getPreference(String id, PreferenceCategory category) {
        Item item = this.preferenceTable.getItem(PREFERENCE_ID_ATTRIBUTE,
                DDBPreferenceAdapter.buildDBStringFromComponents(id, category));
        if (item == null) {
            return null;
        } else {
            return new DDBPreferenceAdapter().withDBModel(item).toObject();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Preference> getCorrelatedPreferences(Preference preference) {
        List<String> keysToGet = new ArrayList<String>();
        
        for (PreferenceCorrelation correlation : preference.getCorrelations()) {
            keysToGet.add(correlation.getToPreferenceID());
        }
        
        Collection<Preference> correlatedPreferences = new ArrayList<Preference>();
        
        TableKeysAndAttributes keysAndAtts = new TableKeysAndAttributes(
                this.preferenceTable.getTableName()).withHashOnlyKeys(
                DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE, keysToGet);
        BatchGetItemSpec spec = new BatchGetItemSpec().withTableKeyAndAttributes(keysAndAtts);
        BatchGetItemOutcome outcome = this.client.batchGetItem(spec);
        
        Map<String, KeysAndAttributes> unprocessedKeys;
        do {
            for (List<Item> items : outcome.getTableItems().values()) {
                for (Item item : items) {
                    correlatedPreferences.add(new DDBPreferenceAdapter().withDBModel(item)
                            .toObject());
                }
            }
            
            unprocessedKeys = outcome.getUnprocessedKeys();
            if (unprocessedKeys != null && unprocessedKeys.size() > 0) {
                outcome = client.batchGetItemUnprocessed(unprocessedKeys);
            }
        } while (unprocessedKeys != null && unprocessedKeys.size() > 0);
        
        return correlatedPreferences;
    }
}
