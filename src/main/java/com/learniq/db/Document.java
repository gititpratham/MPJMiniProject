package com.learniq.db;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a document in the system
 */
public class Document {
    private static final Logger LOGGER = Logger.getLogger(Document.class.getName());
    
    private String id;
    private String title;
    private String filePath;
    private String ownerId;
    private ZonedDateTime createdAt;
    private ZonedDateTime lastAccessed;
    
    /**
     * Creates a new document with the given details
     * 
     * @param title The document title
     * @param filePath The path to the document file
     * @param ownerId The ID of the user who owns the document
     */
    public Document(String title, String filePath, String ownerId) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.filePath = filePath;
        this.ownerId = ownerId;
        this.createdAt = ZonedDateTime.now();
        this.lastAccessed = ZonedDateTime.now();
    }
    
    /**
     * Creates a document from a Neo4j record
     * 
     * @param record The database record
     * @return A new Document object
     */
    public static Document fromRecord(Record record) {
        Document document = new Document();
        document.id = record.get("d.id").asString();
        document.title = record.get("d.title").asString();
        document.filePath = record.get("d.filePath").asString();
        document.ownerId = record.get("u.id").asString();
        document.createdAt = record.get("d.createdAt").asZonedDateTime();
        document.lastAccessed = record.get("d.lastAccessed").asZonedDateTime();
        
        return document;
    }
    
    /**
     * Private constructor for creating a document from a database record
     */
    private Document() {
        // For internal use
    }
    
    /**
     * Saves a new document to the database
     * 
     * @return true if the document was saved successfully, false otherwise
     */
    public boolean save() {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            // Create the document node
            Result result = dbManager.executeWriteQuery(
                "CREATE (d:Document {" +
                "id: $id, " +
                "title: $title, " +
                "filePath: $filePath, " +
                "createdAt: $createdAt, " +
                "lastAccessed: $lastAccessed " +
                "}) RETURN d",
                Map.of(
                    "id", id,
                    "title", title,
                    "filePath", filePath,
                    "createdAt", createdAt,
                    "lastAccessed", lastAccessed
                )
            );
            
            if (!result.hasNext()) {
                return false;
            }
            
            // Create the relationship between user and document
            dbManager.executeWriteQuery(
                "MATCH (u:User {id: $ownerId}), (d:Document {id: $documentId}) " +
                "CREATE (u)-[:OWNS]->(d)",
                Map.of(
                    "ownerId", ownerId,
                    "documentId", id
                )
            );
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving document: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Gets a document by its ID
     * 
     * @param documentId The document ID
     * @return The document or null if not found
     */
    public static Document getById(String documentId) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeQuery(
                "MATCH (u:User)-[:OWNS]->(d:Document {id: $documentId}) " +
                "RETURN d.id, d.title, d.filePath, d.createdAt, d.lastAccessed, u.id",
                Map.of("documentId", documentId)
            );
            
            if (!result.hasNext()) {
                return null;
            }
            
            // Update last accessed
            dbManager.executeWriteQuery(
                "MATCH (d:Document {id: $documentId}) " +
                "SET d.lastAccessed = $lastAccessed",
                Map.of(
                    "documentId", documentId,
                    "lastAccessed", ZonedDateTime.now()
                )
            );
            
            return Document.fromRecord(result.next());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting document: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets all documents for a user
     * 
     * @param userId The user ID
     * @return A list of documents
     */
    public static List<Document> getByUserId(String userId) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeQuery(
                "MATCH (u:User {id: $userId})-[:OWNS]->(d:Document) " +
                "RETURN d.id, d.title, d.filePath, d.createdAt, d.lastAccessed, u.id " +
                "ORDER BY d.lastAccessed DESC",
                Map.of("userId", userId)
            );
            
            return result.list(Document::fromRecord);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting documents for user: " + e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Deletes a document by its ID
     * 
     * @param documentId The document ID
     * @return true if the document was deleted successfully, false otherwise
     */
    public static boolean delete(String documentId) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            Result result = dbManager.executeWriteQuery(
                "MATCH (d:Document {id: $documentId}) " +
                "DETACH DELETE d " +
                "RETURN count(d) as deleted",
                Map.of("documentId", documentId)
            );
            
            if (result.hasNext()) {
                return result.next().get("deleted").asInt() > 0;
            }
            
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting document: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Updates the document's title
     * 
     * @param newTitle The new title
     * @return true if the update was successful, false otherwise
     */
    public boolean updateTitle(String newTitle) {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            dbManager.executeWriteQuery(
                "MATCH (d:Document {id: $id}) " +
                "SET d.title = $title",
                Map.of(
                    "id", id,
                    "title", newTitle
                )
            );
            
            this.title = newTitle;
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating document title: " + e.getMessage(), e);
            return false;
        }
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
    
    public ZonedDateTime getLastAccessed() {
        return lastAccessed;
    }
    
    /**
     * Updates the last accessed time of the document
     */
    public void updateLastAccessed() {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            
            ZonedDateTime now = ZonedDateTime.now();
            
            dbManager.executeWriteQuery(
                "MATCH (d:Document {id: $id}) " +
                "SET d.lastAccessed = $lastAccessed",
                Map.of(
                    "id", id,
                    "lastAccessed", now
                )
            );
            
            this.lastAccessed = now;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating document last accessed time: " + e.getMessage(), e);
        }
    }
}