package data.proxy;

import java.util.HashMap;
import java.util.Map;

import data.structure.UserProfile;

/**
 * LocalTransientUserProfileStore manages access to the stored user profiles locally in memory.
 */
public class LocalTransientUserProfileStore implements UserProfileStore {
    private Map<String, UserProfile> userProfiles;
    
    /**
     * Basic default constructor for LocalTransientUserProfileStore.
     */
    public LocalTransientUserProfileStore() {
        userProfiles = new HashMap<String, UserProfile>();
    }
    
    /**
     * {@inheritDoc}
     */
    public void write(UserProfile profile) {
        userProfiles.put(profile.getId(), profile);
    }
    
    /**
     * {@inheritDoc}
     */
    public void delete(String id) {
        userProfiles.remove(id);
    }
    
    /**
     * {@inheritDoc}
     */
    public UserProfile getProfile(String id) {
        if (!userProfiles.containsKey(id)) {
            return null;
        } else {
            return userProfiles.get(id);
        }
    }
}
