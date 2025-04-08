package com.learniq.db;

import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a user in the system
 */
public class User {
    private static final Logger LOGGER = Logger.getLogger(User.class.getName());
    
    private String id;
    private String email;
    private String fullName;
    private String passwordHash;
    private List<String> documentIds;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastLogin;
    
    /**
     * Creates a new user object with the given details
     * 
     * @param email The user's email address
     * @param fullName The user's full name
     * @param password The user's plaintext password (will be hashed)
     */
    public User(String email, String fullName, String password) {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = hashPassword(password);
        this.documentIds = new ArrayList<>();
        this.createdAt = ZonedDateTime.now();
        this.lastLogin = ZonedDateTime.now();
    }
    
    /**
     * Creates a user from a Neo4j record
     * 
     * @param record The database record
     * @return A new User object
     */
    public static User fromRecord(Record record) {
        User user = new User();
        user.id = record.get("u.id").asString();
        user.email = record.get("u.email").asString();
        user.fullName = record.get("u.fullName").asString();
        user.passwordHash = record.get("u.passwordHash").asString();
        user.createdAt = record.get("u.createdAt").asZonedDateTime();
        user.lastLogin = record.get("u.lastLogin").asZonedDateTime();
        
        // Get document IDs if they exist
        user.documentIds = new ArrayList<>();
        if (record.get("documentIds").isNull()) {
            user.documentIds = new ArrayList<>();
        } else {
            record.get("documentIds").asList(value -> value.asString());
        }
        
        return user;
    }
    
    /**
     * Private constructor for creating a user from a database record
     */
    private User() {
        // For internal use
    }
    
    /**
     * Saves a new user to the database
     * 
     * @return true if the user was saved successfully, false otherwise
     */
    public boolean save() {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeWriteQuery(
                "CREATE (u:User {" +
                "id: $id, " +
                "email: $email, " +
                "fullName: $fullName, " +
                "passwordHash: $passwordHash, " +
                "createdAt: $createdAt, " +
                "lastLogin: $lastLogin " +
                "}) RETURN u",
                Map.of(
                    "id", id,
                    "email", email,
                    "fullName", fullName,
                    "passwordHash", passwordHash,
                    "createdAt", createdAt,
                    "lastLogin", lastLogin
                )
            );
            
            return result.hasNext();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving user: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Authenticate a user with email and password
     * 
     * @param email The user's email
     * @param password The user's plaintext password
     * @return The authenticated user or null if authentication fails
     */
    public static User authenticate(String email, String password) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeQuery(
                "MATCH (u:User {email: $email}) " +
                "OPTIONAL MATCH (u)-[:OWNS]->(d:Document) " +
                "RETURN u.id, u.email, u.fullName, u.passwordHash, u.createdAt, u.lastLogin, " +
                "collect(d.id) as documentIds",
                Map.of("email", email)
            );
            
            if (!result.hasNext()) {
                return null; // User not found
            }
            
            Record record = result.next();
            String storedHash = record.get("u.passwordHash").asString();
            
            // Check if the password matches
            if (!checkPassword(password, storedHash)) {
                return null; // Wrong password
            }
            
            // Update last login time
            dbManager.executeWriteQuery(
                "MATCH (u:User {email: $email}) " +
                "SET u.lastLogin = $lastLogin",
                Map.of(
                    "email", email,
                    "lastLogin", ZonedDateTime.now()
                )
            );
            
            return User.fromRecord(record);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during authentication: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Add a document ID to the user's documents
     * 
     * @param documentId The document ID to add
     * @return true if the document was added successfully, false otherwise
     */
    public boolean addDocument(String documentId) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            dbManager.executeWriteQuery(
                "MATCH (u:User {id: $userId}), (d:Document {id: $documentId}) " +
                "CREATE (u)-[:OWNS]->(d)",
                Map.of(
                    "userId", id,
                    "documentId", documentId
                )
            );
            
            documentIds.add(documentId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding document to user: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Updates the user's information in the database
     * 
     * @param newFullName The new full name (null to keep current)
     * @param newPassword The new password (null to keep current)
     * @return true if the update was successful, false otherwise
     */
    public boolean update(String newFullName, String newPassword) {
        try {
            Map<String, Object> params = Map.of(
                "id", id,
                "fullName", newFullName != null ? newFullName : fullName,
                "passwordHash", newPassword != null ? hashPassword(newPassword) : passwordHash
            );
            
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            dbManager.executeWriteQuery(
                "MATCH (u:User {id: $id}) " +
                "SET u.fullName = $fullName, u.passwordHash = $passwordHash",
                params
            );
            
            // Update the object
            if (newFullName != null) {
                this.fullName = newFullName;
            }
            
            if (newPassword != null) {
                this.passwordHash = hashPassword(newPassword);
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating user: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a user with the given email exists
     * 
     * @param email The email to check
     * @return true if a user with the email exists, false otherwise
     */
    public static boolean emailExists(String email) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeQuery(
                "MATCH (u:User {email: $email}) RETURN count(u) as count",
                Map.of("email", email)
            );
            
            if (result.hasNext()) {
                Record record = result.next();
                return record.get("count").asInt() > 0;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error checking if email exists: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Hashes a password using BCrypt
     * 
     * @param password The plaintext password
     * @return The hashed password
     */
    private static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    
    /**
     * Checks if a password matches the hashed version
     * 
     * @param password The plaintext password
     * @param hashedPassword The hashed password
     * @return true if the password matches, false otherwise
     */
    private static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public List<String> getDocumentIds() {
        return new ArrayList<>(documentIds);
    }
    
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    public ZonedDateTime getLastLogin() {
        return lastLogin;
    }
}