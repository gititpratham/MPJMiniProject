package com.learniq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Client for interacting with Google's Gemini API for text generation
 */
public class GeminiClient {
    private final String apiKey;
    private final String model;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/%s:generateContent?key=%s";
    
    /**
     * Creates a new client for the Gemini API
     * 
     * @param apiKey The API key to authenticate with
     * @param model The model to use (e.g., "models/gemini-1.5-pro")
     */
    public GeminiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
    
    /**
     * Creates a new client for the Gemini API with the default model
     * 
     * @param apiKey The API key to authenticate with
     */
    public GeminiClient(String apiKey) {
        this(apiKey, "models/gemini-1.5-pro");
    }
    
    /**
     * Generates a quiz based on the provided context
     * 
     * @param context The text context to generate questions from
     * @return A string containing the generated quiz
     * @throws IOException If the API request fails
     */
    public String generateQuiz(String context) throws IOException {
        String prompt = String.format(
            "Based on this text from a book, create 5 multiple choice questions.%n" +
            "For each question, specify if it's [BEGINNER], [INTERMEDIATE], or [ADVANCED].%n" +
            "Make 2 beginner, 2 intermediate, and 1 advanced question.%n%n" +
            "Format each question exactly like this:%n" +
            "1. [BEGINNER] What is...?%n" +
            "A) First option%n" +
            "B) Second option%n" +
            "C) Third option%n" +
            "D) Fourth option%n" +
            "Correct: A%n%n" +
            "Text content:%n%s%n%n" +
            "Generate 5 questions following the exact format above:", context);
            
        return generateText(prompt);
    }
    
    /**
     * Generates an adaptive response based on the user's skill level
     * 
     * @param question The user's question
     * @param context The relevant context from the document
     * @param level The user's skill level (beginner, intermediate, advanced)
     * @return A response tailored to the user's skill level
     * @throws IOException If the API request fails
     */
    public String generateAdaptiveResponse(String question, String context, String level) throws IOException {
        Map<String, String> prompts = new HashMap<>();
        
        prompts.put("beginner", 
            "You are explaining to a beginner. Use simple English and provide step-by-step explanations with examples. " +
            "Include bullet points for clarity.%n%n" +
            "Context:%n%s%n%n" +
            "Question: %s%n%n" +
            "Give a beginner-friendly response:");
            
        prompts.put("intermediate", 
            "Context:%n%s%n%n" +
            "Question: %s%n%n" +
            "Provide a balanced response with some technical details:");
            
        prompts.put("advanced", 
            "You are explaining to an advanced user. Be concise and include advanced concepts. " +
            "Also suggest a related challenging question for them to think about.%n%n" +
            "Context:%n%s%n%n" +
            "Question: %s%n%n" +
            "Provide an advanced response and a follow-up question:");
        
        String promptTemplate = prompts.getOrDefault(level.toLowerCase(), prompts.get("intermediate"));
        String prompt = String.format(promptTemplate, context, question);
        
        return generateText(prompt);
    }
    
    /**
     * Generates text using the Gemini API
     * 
     * @param prompt The prompt to generate text from
     * @return The generated text
     * @throws IOException If the API request fails
     */
    public String generateText(String prompt) throws IOException {
        // Create the URL with the model and API key
        URL url = new URL(String.format(API_URL, model, apiKey));
        
        // Set up the connection
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        
        // Create the request body
        String requestBody = createRequestBody(prompt);
        
        // Send the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // Get the response
        int responseCode = connection.getResponseCode();
        
        // Check if the request was successful
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String jsonResponse = br.lines().collect(Collectors.joining(System.lineSeparator()));
                return parseTextFromResponse(jsonResponse);
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String error = br.lines().collect(Collectors.joining(System.lineSeparator()));
                throw new IOException("API request failed with code " + responseCode + ": " + error);
            }
        }
    }
    
    /**
     * Creates the JSON request body for the Gemini API
     * 
     * @param prompt The prompt to generate text from
     * @return The JSON request body as a string
     */
    private String createRequestBody(String prompt) {
        JSONObject requestBody = new JSONObject();
        
        // Set generation config
        requestBody.put("generationConfig", new JSONObject()
            .put("temperature", 0.7)
            .put("maxOutputTokens", 2048)
            .put("topP", 0.95)
            .put("topK", 40));
        
        // Build the content object with the prompt
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt));
        content.put("parts", parts);
        
        // Add to contents array
        JSONArray contents = new JSONArray();
        contents.put(content);
        requestBody.put("contents", contents);
        
        return requestBody.toString();
    }
    
    /**
     * Parses the text from the Gemini API response
     * 
     * @param jsonResponse The JSON response from the API
     * @return The generated text
     * @throws IOException If the response cannot be parsed
     */
    private String parseTextFromResponse(String jsonResponse) throws IOException {
        try {
            JSONObject response = new JSONObject(jsonResponse);
            
            if (!response.has("candidates") || response.getJSONArray("candidates").length() == 0) {
                throw new IOException("No candidates found in response");
            }
            
            JSONObject candidate = response.getJSONArray("candidates").getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            
            if (parts.length() == 0) {
                throw new IOException("No parts found in response");
            }
            
            return parts.getJSONObject(0).getString("text");
        } catch (Exception e) {
            throw new IOException("Failed to parse API response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Escapes special characters in a string for use in JSON
     * 
     * @param str The string to escape
     * @return The escaped string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    /**
     * Unescapes special characters in a JSON string
     * 
     * @param str The string to unescape
     * @return The unescaped string
     */
    private String unescapeJson(String str) {
        return str.replace("\\\"", "\"")
                 .replace("\\n", "\n")
                 .replace("\\r", "\r")
                 .replace("\\t", "\t")
                 .replace("\\\\", "\\");
    }
}