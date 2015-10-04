package data.proxy;

import data.structure.UserProfile;

/**
 * UserProfileStore manages access to the stored user profiles.
 */
public abstract interface UserProfileStore {
    
    /**
     * Writes a user profile to storage.
     * 
     * @param profile The UserProfile object to store
     */
    public void write(UserProfile profile);
    
    /**
     * Remove a user profile from storage.
     *
     * @param id The String id for the UserProfile to delete
     */
    public void delete(String id);
    
    /**
     * Gets the profile for the specified user from storage.
     * 
     * @param id The String id for the UserProfile to retrieve
     * @return the user's profile, or null if the user does not exist
     */
    public UserProfile getProfile(String id);
}
