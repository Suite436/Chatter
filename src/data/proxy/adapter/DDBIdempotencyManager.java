package data.proxy.adapter;

import java.util.List;

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.Expected;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;

import data.proxy.request.UpdatePreferenceRequest.UpdateAction;
import data.structure.UserProfile;

/**
 * DDBIdempotencyManager encapsulates the functionality required to guarantee idempotency in a
 * DynamoDB transaction.
 */
public class DDBIdempotencyManager {
    
    private static final String LAST_MODIFIED_BY_ATTRIBUTE = "LastModifiedBy";
    private static final String SEPARATOR = "~~";
    
    /**
     * Builds a unique String that will be used to identify the last transaction that modified a
     * DynamoDB Item. This will be tested as the condition for idempotency (ie. this flag must not
     * be the same as the value of the LAST_MODIFIED_BY attribute of the Item).
     * 
     * @param user
     * @param updates
     * @param action
     * @return idempotency flag value
     */
    private static String buildIdempotencyFlag(UserProfile user, List<AttributeUpdate> updates,
            UpdateAction action) {
        StringBuilder builder = new StringBuilder();
        for (AttributeUpdate update : updates) {
            builder.append(update.getAttributeName());
        }
        return String.format("%s%s%s%s%s", user.getId(), SEPARATOR, builder.toString(), SEPARATOR,
                action.getDelta());
    }
    
    /**
     * Builds the Expectation that will be used to guarantee idempotency.
     * 
     * @param flagVal
     * @return expectation
     */
    private static Expected buildIdempotencyExpectation(String flagVal) {
        return new Expected(LAST_MODIFIED_BY_ATTRIBUTE).ne(flagVal);
    }
    
    /**
     * Adds the condition to guarantee idempotency to an UpdateItemSpec.
     * 
     * @param spec
     * @param user
     * @param action
     * @return spec
     */
    public static UpdateItemSpec makeUpdateIdempotent(UpdateItemSpec spec, UserProfile user,
            UpdateAction action) {
        // Build flag String
        String flagVal = buildIdempotencyFlag(user, spec.getAttributeUpdate(), action);
        
        // Add condition that the existing flag must not equal flagVal
        spec.withExpected(buildIdempotencyExpectation(flagVal));
        
        // Add update entry to set flag
        spec.withAttributeUpdate(new AttributeUpdate(LAST_MODIFIED_BY_ATTRIBUTE).put(flagVal));
        
        return spec;
    }
}
