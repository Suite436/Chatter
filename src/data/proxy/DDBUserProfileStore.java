package data.proxy;

import static data.proxy.adapter.DDBUserProfileAdapter.USER_ID_ATTRIBUTE;

import java.util.List;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import data.proxy.adapter.DDBUserProfileAdapter;
import data.structure.UserProfile;

/**
 * DDBUserProfileStore manages access to the stored user profiles using a DynamoDB client.
 */
public class DDBUserProfileStore implements UserProfileStore {
    
    private final Table userTable;
    
    /**
     * Constructor requires a client and a table name, both of which are validated.
     * 
     * @param client
     * @param userTable
     */
    public DDBUserProfileStore(DynamoDB client, String userTable) {
        try {
            Table table = client.getTable(userTable);
            validateTableDescription(table.describe());
            this.userTable = table;
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
            if (USER_ID_ATTRIBUTE.equals(element.getAttributeName())) {
                return;
            }
        }
        throw new IllegalArgumentException(String.format(
                "The table \'%s\' is not formatted correctly!", tableDesc.getTableName()));
    }
    
    /**
     * {@inheritDoc}
     */
    public void write(UserProfile profile) {
        Item item = new DDBUserProfileAdapter().withObject(profile).toDBModel();
        this.userTable.putItem(item);
    }
    
    /**
     * {@inheritDoc}
     */
    public void delete(String id) {
        this.userTable.deleteItem(USER_ID_ATTRIBUTE, id);
        
    }
    
    /**
     * {@inheritDoc}
     */
    public UserProfile getProfile(String id) {
        Item item = this.userTable.getItem(USER_ID_ATTRIBUTE, id);
        if (item == null) {
            return null;
        } else {
            return new DDBUserProfileAdapter().withDBModel(item).toObject();
        }
    }
}
