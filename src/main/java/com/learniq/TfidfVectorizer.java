package com.learniq;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Implements a simplified TF-IDF vectorization and similarity calculations
 * for finding relevant text chunks, without external dependencies
 */
public class TfidfVectorizer {
    private final Pattern nonAlphanumeric = Pattern.compile("[^a-zA-Z0-9]");
    
    /**
     * Gets the most relevant chunks of text for a given query
     * 
     * @param query The search query
     * @param chunks The list of text chunks to search through
     * @param topK The number of chunks to return
     * @return List of the most relevant text chunks
     */
    public List<String> getRelevantChunks(String query, List<String> chunks, int topK) {
        // Simple implementation: look for keyword matches with query
        List<String> allChunks = new ArrayList<>(chunks);
        List<String> topChunks = new ArrayList<>();
        
        // Normalize and split query
        String[] queryWords = normalizeText(query);
        
        // Score each chunk based on word overlap with query
        Map<String, Double> scores = new HashMap<>();
        
        for (String chunk : chunks) {
            String[] chunkWords = normalizeText(chunk);
            double score = calculateOverlapScore(queryWords, chunkWords);
            scores.put(chunk, score);
        }
        
        // Sort chunks by score and take top K
        allChunks.sort((a, b) -> Double.compare(scores.getOrDefault(b, 0.0), scores.getOrDefault(a, 0.0)));
        
        // Take top K results
        int resultSize = Math.min(topK, allChunks.size());
        for (int i = 0; i < resultSize; i++) {
            topChunks.add(allChunks.get(i));
        }
        
        return topChunks;
    }
    
    /**
     * Normalizes and tokenizes text into an array of words
     * 
     * @param text The text to process
     * @return Array of normalized words
     */
    private String[] normalizeText(String text) {
        // Convert to lowercase
        String lowerText = text.toLowerCase();
        
        // Remove non-alphanumeric characters and replace with spaces
        String cleanText = nonAlphanumeric.matcher(lowerText).replaceAll(" ");
        
        // Split into words and filter out short words
        return Arrays.stream(cleanText.split("\\s+"))
            .filter(word -> word.length() > 2) // Filter out short words
            .toArray(String[]::new);
    }
    
    /**
     * Calculates a simple overlap score between query and chunk words
     * 
     * @param queryWords Array of query words
     * @param chunkWords Array of chunk words
     * @return A similarity score
     */
    private double calculateOverlapScore(String[] queryWords, String[] chunkWords) {
        // Count word occurrences in chunk
        Map<String, Integer> chunkWordCounts = new HashMap<>();
        for (String word : chunkWords) {
            chunkWordCounts.put(word, chunkWordCounts.getOrDefault(word, 0) + 1);
        }
        
        // Calculate score based on query terms found in chunk
        double score = 0.0;
        for (String queryWord : queryWords) {
            score += chunkWordCounts.getOrDefault(queryWord, 0);
        }
        
        // Normalize by chunk length to avoid bias towards longer chunks
        return score / Math.sqrt(chunkWords.length);
    }
}