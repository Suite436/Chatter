package server.standalone;

import java.util.Scanner;

import server.daemons.UpdatePreferenceDaemon;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

import data.proxy.DDBPreferenceCorrelationGraph;
import data.proxy.DDBUserProfileStore;
import data.proxy.PreferenceCorrelationGraph;
import data.proxy.UserProfileStore;
import data.structure.Preference;
import data.structure.PreferenceCategory;
import data.structure.UserProfile;

/**
 * This class acts as a standalone runner for the Chatter server. It also surfaces a simple CLI.
 */
public class SimpleServerCLI {
    
    /**
     * The set of allowed commands for this simple CLI.
     */
    enum COMMAND {
        LOGIN, ADD, REMOVE
    };
    
    private static final Scanner in = new Scanner(System.in);
    private static UserProfile currentUser;
    
    /**
     * Main driver method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        final UserProfileStore userStore = new DDBUserProfileStore(new DynamoDB(
                new AmazonDynamoDBClient()), "UserProfiles");
        // final UserProfileStore userStore = new LocalTransientUserProfileStore();
        final PreferenceCorrelationGraph preferenceGraph = new DDBPreferenceCorrelationGraph(
                new DynamoDB(new AmazonDynamoDBClient()), "PreferenceCorrelations");
        // final PreferenceCorrelationGraph preferenceGraph = new
        // LocalTransientPreferenceCorrelationGraph();
        final UpdatePreferenceDaemon updater = new UpdatePreferenceDaemon(preferenceGraph);
        
        printGreeting();
        
        while (true) {
            System.out.print(">> ");
            String[] line = parseCommand(in.nextLine());
            
            if (line.length == 0) {
                continue;
            }
            
            COMMAND cmd = null;
            try {
                cmd = COMMAND.valueOf(line[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println(String.format("Invalid command \"%s\". Valid options are %s",
                        line[0], COMMAND.values().toString()));
                continue;
            }
            
            switch (cmd) {
            case LOGIN:
                login(userStore, line);
                break;
            case ADD:
                addPreference(userStore, updater, line);
                break;
            case REMOVE:
                removePreference(userStore, updater, line);
                break;
            default:
            }
        }
    }
    
    /**
     * Prints the opening message.
     */
    private static void printGreeting() {
        System.out.println("=======================================");
        System.out.println("Welcome to the Chatter SimpleServerCLI!");
        System.out.println("=======================================");
        System.out.println("**Available commands (Group multi-word args with double quotes):");
        System.out.println(">> login {username}");
        System.out.println(">> add {preference category} {preference name}");
        System.out.println("=======================================\r\n");
    }
    
    private static final int USER_ID_INDEX = 1;
    
    /**
     * Logs in with the specified user profile or creates a new profile if necessary.
     * 
     * @param userStore
     * @param line
     */
    private static void login(UserProfileStore userStore, String[] line) {
        String userId = line[USER_ID_INDEX];
        
        currentUser = userStore.getProfile(userId);
        
        if (currentUser == null) {
            System.out.println(String.format("Creating new user %s.", userId));
            userStore.write(new UserProfile(userId));
            currentUser = userStore.getProfile(userId);
        }
        
        System.out.println(String.format("Logged in as %s.", userId));
    }
    
    private static final int PREFERENCE_CATEGORY_INDEX = 1;
    private static final int PREFERENCE_ID_INDEX = 2;
    
    /**
     * Adds a preference for the current user.
     * 
     * @param userStore
     * @param updater
     * @param line
     */
    private static void addPreference(UserProfileStore userStore, UpdatePreferenceDaemon updater,
            String[] line) {
        if (isLoggedIn()) {
            String categoryString = line[PREFERENCE_CATEGORY_INDEX].trim().toUpperCase();
            PreferenceCategory category = PreferenceCategory.valueOf(categoryString);
            String preferenceId = line[PREFERENCE_ID_INDEX];
            
            Preference addedPreference = currentUser.addPreference(category, preferenceId);
            if (addedPreference != null) {
                userStore.write(currentUser);
                updater.propagateAddedPreference(currentUser, addedPreference);
                
                System.out.println(String.format("Added preference %s: %s.", categoryString,
                        preferenceId));
            }
        }
    }
    
    /**
     * Removes a preference for the current user.
     * 
     * @param userStore
     * @param updater
     * @param line
     */
    private static void removePreference(UserProfileStore userStore,
            UpdatePreferenceDaemon updater, String[] line) {
        if (isLoggedIn()) {
            String categoryString = line[PREFERENCE_CATEGORY_INDEX].toUpperCase();
            PreferenceCategory category = PreferenceCategory.valueOf(categoryString);
            String preferenceId = line[PREFERENCE_ID_INDEX];
            
            Preference removedPreference = currentUser.removePreference(category, preferenceId);
            if (removedPreference != null) {
                userStore.write(currentUser);
                updater.propagateRemovedPreference(currentUser, removedPreference);
                
                System.out.println(String.format("Removed preference %s: %s.", categoryString,
                        preferenceId));
            }
        }
    }
    
    /**
     * Checks to see if the user is currently logged in.
     * 
     * @return
     */
    private static boolean isLoggedIn() {
        if (currentUser == null) {
            System.out.println("Please log in first.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Parses the entire command into arguments, respecting double-quote groupings by user.
     * 
     * @param line
     * @return
     */
    private static String[] parseCommand(String line) {
        final String ARG_SEPARATOR = "~!~";
        StringBuilder finalArgs = new StringBuilder();
        
        // Split by double quotes
        String[] quoteSplit = line.split("\"");
        
        // The first segment must begin with the command itself, and
        // any other components in here are also single-word arguments.
        String[] starterSegment = quoteSplit[0].split("\\s");
        for (String component : starterSegment) {
            finalArgs.append(component);
            finalArgs.append(ARG_SEPARATOR);
        }
        
        // Now, iterate through the rest of the segments, flipping
        // quote groupings on and off as appropriate
        boolean insideQuotes = true;
        for (int i = 1; i < quoteSplit.length; i++) {
            if (insideQuotes) {
                // If we are currently inside quotes, store the entire segment as an argument.
                finalArgs.append(quoteSplit[i]);
                finalArgs.append(ARG_SEPARATOR);
            } else {
                // If we are not inside quotes, we want to split by whitespace.
                String[] singleWordArgs = quoteSplit[i].split("\\s");
                for (String component : singleWordArgs) {
                    finalArgs.append(component);
                    finalArgs.append(ARG_SEPARATOR);
                }
            }
            
            // We want to flip our insideQuotes flag for each quote-split seqment.
            insideQuotes = !insideQuotes;
        }
        
        return finalArgs.toString().split(ARG_SEPARATOR);
        
    }
}
