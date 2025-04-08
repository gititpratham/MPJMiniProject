package com.learniq.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for database operations
 */
public class DbUtils {
    private static final Logger LOGGER = Logger.getLogger(DbUtils.class.getName());
    private static final String DOCUMENTS_FOLDER = "user_documents";
    
    /**
     * Saves a file to the user documents folder and creates a document in the database
     * 
     * @param filePath The path to the file to save
     * @param fileName The name of the file
     * @param userId The ID of the user who owns the file
     * @return The document ID if successful, null otherwise
     */
    public static String saveUserDocument(String filePath, String fileName, String userId) {
        try {
            // Create the documents folder if it doesn't exist
            Path documentsPath = Paths.get(DOCUMENTS_FOLDER);
            if (!Files.exists(documentsPath)) {
                Files.createDirectories(documentsPath);
            }
            
            // Generate a unique filename
            String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
            Path targetPath = documentsPath.resolve(uniqueFileName);
            
            // Copy the file
            Files.copy(Paths.get(filePath), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create a document in the database
            Document document = new Document(fileName, targetPath.toString(), userId);
            if (document.save()) {
                return document.getId();
            } else {
                // Delete the file if the document couldn't be saved
                Files.deleteIfExists(targetPath);
                return null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving user document: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Deletes a document and its file
     * 
     * @param documentId The ID of the document to delete
     * @return true if the document was deleted successfully, false otherwise
     */
    public static boolean deleteUserDocument(String documentId) {
        try {
            // Get the document
            Document document = Document.getById(documentId);
            if (document == null) {
                return false;
            }
            
            // Delete the file
            Files.deleteIfExists(Paths.get(document.getFilePath()));
            
            // Delete the document from the database
            return Document.delete(documentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting user document: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Formats a date for display
     * 
     * @param dateTime The date to format
     * @return The formatted date
     */
    public static String formatDate(ZonedDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"));
    }
    
    /**
     * Checks if the database is connected
     * 
     * @return true if the database is connected, false otherwise
     */
    public static boolean isDatabaseConnected() {
        try {
            Neo4jManager dbManager = Neo4jManager.getInstance();
            return dbManager.testConnection();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database connection error: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Creates the documents folder if it doesn't exist
     * 
     * @return true if the folder exists or was created successfully, false otherwise
     */
    public static boolean ensureDocumentsFolderExists() {
        try {
            Path documentsPath = Paths.get(DOCUMENTS_FOLDER);
            if (!Files.exists(documentsPath)) {
                Files.createDirectories(documentsPath);
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating documents folder: " + e.getMessage(), e);
            return false;
        }
    }
}