package data.proxy;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import data.proxy.adapter.DDBPreferenceAdapter;
import data.structure.Preference;
import data.structure.PreferenceCategory;

/**
 * Tests the functionality of the DDBPreferenceCorrelationGraph class.
 */
public class DDBPreferenceCorrelationGraphTest {
    
    private static final String PREFERENCE_TABLE_NAME = "PreferenceCorrelations";
    private static final String NONEXISTENT_TABLE_NAME = "PigsThatHaveFlown";
    private static final String INVALID_SCHEMA_TABLE_NAME = "UserPosts";
    private static final String INVALID_SCHEMA_KEY_NAME = "PancakeID";
    private static final String TEST_PREFERENCE_ID = "TestPreference";
    private static final PreferenceCategory TEST_PREFERENCE_CATEGORY = PreferenceCategory.TELEVISION;
    private static final String TEST_HASH_KEY = DDBPreferenceAdapter.buildDBStringFromComponents(
            TEST_PREFERENCE_ID, TEST_PREFERENCE_CATEGORY);
    private DynamoDB ddbClient;
    
    /**
     * Creates the basic mock DynamoDB object.
     */
    @Before
    public void setup() {
        ddbClient = createMock(DynamoDB.class);
    }
    
    /**
     * Tests the constructor requirement for a non-null DynamoDB client.
     */
    @Test
    public void testConstructorNullClient() {
        boolean thrown = false;
        
        try {
            new DDBPreferenceCorrelationGraph(null, PREFERENCE_TABLE_NAME);
        } catch (NullPointerException e) {
            thrown = true;
        }
        
        assertTrue("A null DynamoDB client was passed in, but no exception was thrown!", thrown);
    }
    
    /**
     * Tests the constructor requirement for a valid DynamoDB table name.
     */
    @Test
    public void testConstructorInvalidTableName() {
        expectNonExistentTable();
        replay(ddbClient);
        
        boolean thrown = false;
        
        try {
            new DDBPreferenceCorrelationGraph(ddbClient, NONEXISTENT_TABLE_NAME);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("An invalid table name was passed in, but no exception was thrown!", thrown);
        
        verify(ddbClient);
    }
    
    /**
     * Tests the constructor requirement for a valid DynamoDB table schema.
     */
    @Test
    public void testConstructorInvalidTableSchema() {
        Table tableToTest = expectInvalidSchemaTable();
        replay(tableToTest);
        replay(ddbClient);
        
        boolean thrown = false;
        
        try {
            new DDBPreferenceCorrelationGraph(ddbClient, INVALID_SCHEMA_TABLE_NAME);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        
        assertTrue("A table with an invalid schema was passed in, but no exception was thrown!",
                thrown);
        
        verify(tableToTest);
        verify(ddbClient);
    }
    
    /**
     * Tests that the write() method of DDBPreferenceCorrelationGraph calls table.putItem() once.
     */
    @Test
    public void testWrite() {
        Table tableToTest = expectValidTable();
        expect(tableToTest.putItem(isA(Item.class))).andReturn(
                new PutItemOutcome(new PutItemResult())).once();
        replay(tableToTest);
        replay(ddbClient);
        
        DDBPreferenceCorrelationGraph graph = new DDBPreferenceCorrelationGraph(ddbClient,
                PREFERENCE_TABLE_NAME);
        graph.write(new Preference(TEST_PREFERENCE_ID, TEST_PREFERENCE_CATEGORY));
        
        verify(tableToTest);
        verify(ddbClient);
    }
    
    /**
     * Tests that the getPreference() method of DDBPreferenceCorrelationGraph calls table.getItem()
     * once.
     */
    @Test
    public void testGet() {
        Table tableToTest = expectValidTable();
        Item testUserItem = new Item().withPrimaryKey(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE,
                TEST_HASH_KEY).withInt(DDBPreferenceAdapter.POPULARITY_ATTRIBUTE, 1);
        expect(tableToTest.getItem(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE, TEST_HASH_KEY))
                .andReturn(testUserItem).once();
        replay(tableToTest);
        replay(ddbClient);
        
        DDBPreferenceCorrelationGraph graph = new DDBPreferenceCorrelationGraph(ddbClient,
                PREFERENCE_TABLE_NAME);
        Preference testPreference = graph.getPreference(TEST_PREFERENCE_ID,
                TEST_PREFERENCE_CATEGORY);
        
        assertNotNull("The getPreference() method did not return a preference!", testPreference);
        assertEquals("The returned profile did not have the correct ID!", TEST_PREFERENCE_ID,
                testPreference.getID());
        
        verify(tableToTest);
        verify(ddbClient);
    }
    
    /**
     * Tests that the delete() method of DDBPreferenceCorrelationGraph calls table.deleteItem()
     * once.
     */
    @Test
    public void testDelete() {
        Table tableToTest = expectValidTable();
        expect(tableToTest.deleteItem(DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE, TEST_HASH_KEY))
                .andReturn(new DeleteItemOutcome(new DeleteItemResult())).once();
        replay(tableToTest);
        replay(ddbClient);
        
        DDBPreferenceCorrelationGraph graph = new DDBPreferenceCorrelationGraph(ddbClient,
                PREFERENCE_TABLE_NAME);
        graph.delete(TEST_PREFERENCE_ID, TEST_PREFERENCE_CATEGORY);
        
        verify(tableToTest);
        verify(ddbClient);
    }
    
    /**
     * Performs setup to expect a valid table.
     * 
     * @return
     */
    private Table expectValidTable() {
        KeySchemaElement keySchema = new KeySchemaElement(
                DDBPreferenceAdapter.PREFERENCE_ID_ATTRIBUTE, KeyType.HASH);
        TableDescription tableDesc = new TableDescription().withTableName(PREFERENCE_TABLE_NAME)
                .withKeySchema(keySchema);
        
        Table userTable = createMock(Table.class);
        expect(userTable.describe()).andReturn(tableDesc).atLeastOnce();
        
        expect(ddbClient.getTable(PREFERENCE_TABLE_NAME)).andReturn(userTable).atLeastOnce();
        
        return userTable;
    }
    
    /**
     * Performs setup to expect a non-existent table.
     */
    private void expectNonExistentTable() {
        expect(ddbClient.getTable(NONEXISTENT_TABLE_NAME)).andThrow(
                new ResourceNotFoundException("Resource Not Found!")).atLeastOnce();
    }
    
    /**
     * Performs setup to expect an existing table that has an invalid table schema.
     * 
     * @return
     */
    private Table expectInvalidSchemaTable() {
        KeySchemaElement invalidKeySchema = new KeySchemaElement(INVALID_SCHEMA_KEY_NAME,
                KeyType.HASH);
        TableDescription invalidTableDesc = new TableDescription().withTableName(
                INVALID_SCHEMA_TABLE_NAME).withKeySchema(invalidKeySchema);
        
        Table invalidTable = createMock(Table.class);
        expect(invalidTable.describe()).andReturn(invalidTableDesc).atLeastOnce();
        
        expect(ddbClient.getTable(INVALID_SCHEMA_TABLE_NAME)).andReturn(invalidTable).atLeastOnce();
        
        return invalidTable;
    }
}
