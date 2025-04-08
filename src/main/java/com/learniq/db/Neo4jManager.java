package com.learniq.db;

import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.Neo4jException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the Neo4j database connection and provides methods for database operations
 */
public class Neo4jManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(Neo4jManager.class.getName());
    private final Driver driver;
    private static Neo4jManager instance;
    
    /**
     * Creates a new Neo4j database manager
     * 
     * @param uri The URI of the Neo4j server
     * @param username The username to connect with
     * @param password The password to connect with
     */
    private Neo4jManager(String uri, String username, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
    
    /**
     * Get the singleton instance of the Neo4j manager
     * 
     * @return The Neo4j manager instance
     */
    public static synchronized Neo4jManager getInstance() {
        if (instance == null) {
            String uri = System.getenv("NEO4J_URI");
            String username = System.getenv("NEO4J_USERNAME");
            String password = System.getenv("NEO4J_PASSWORD");
            
            if (uri == null || username == null || password == null) {
                LOGGER.log(Level.SEVERE, "Neo4j environment variables not set. Please set NEO4J_URI, NEO4J_USERNAME, and NEO4J_PASSWORD.");
                throw new RuntimeException("Neo4j environment variables not set.");
            }
            
            instance = new Neo4jManager(uri, username, password);
            
            // Initialize the database if needed
            instance.initializeDatabase();
        }
        
        return instance;
    }
    
    /**
     * Closes the database driver
     */
    @Override
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
    
    /**
     * Executes a read query
     * 
     * @param query The Cypher query to execute
     * @param parameters The query parameters
     * @return A result that can be used to retrieve the query data
     */
    public Result executeQuery(String query, Map<String, Object> parameters) {
        try (Session session = driver.session()) {
            return session.run(query, parameters);
        }
    }
    
    /**
     * Executes a write query in a transaction
     * 
     * @param query The Cypher query to execute
     * @param parameters The query parameters
     * @return A result that can be used to retrieve the query data
     */
    public Result executeWriteQuery(String query, Map<String, Object> parameters) {
        try (Session session = driver.session()) {
            return session.executeWrite(tx -> tx.run(query, parameters));
        }
    }
    
    /**
     * Initializes the database with required constraints and indexes
     */
    private void initializeDatabase() {
        try {
            // Create constraints for users
            executeWriteQuery(
                "CREATE CONSTRAINT user_email_unique IF NOT EXISTS " +
                "FOR (u:User) REQUIRE u.email IS UNIQUE",
                Map.of()
            );
            
            // Create constraint for documents
            executeWriteQuery(
                "CREATE CONSTRAINT document_id_unique IF NOT EXISTS " +
                "FOR (d:Document) REQUIRE d.id IS UNIQUE",
                Map.of()
            );
            
            // Create indexes for faster lookups
            executeWriteQuery(
                "CREATE INDEX user_email_index IF NOT EXISTS FOR (u:User) ON (u.email)",
                Map.of()
            );
            
            LOGGER.info("Database initialized successfully with constraints and indexes.");
        } catch (Neo4jException e) {
            LOGGER.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Tests the database connection
     * 
     * @return true if the connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            executeQuery("RETURN 1", Map.of());
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed: " + e.getMessage(), e);
            return false;
        }
    }
}