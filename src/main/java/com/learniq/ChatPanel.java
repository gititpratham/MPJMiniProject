package com.learniq;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

/**
 * Panel for displaying chat messages and interacting with the AI
 */
public class ChatPanel extends JPanel {
    private final GeminiClient geminiClient;
    private final TfidfVectorizer vectorizer;
    private final List<String> textChunks;
    private final String userLevel;
    
    private final JTextArea chatDisplay;
    private final JTextField userInput;
    private final JButton sendButton;
    private final JScrollPane scrollPane;
    
    /**
     * Creates a new chat panel
     * 
     * @param geminiClient The Gemini client to use for generating responses
     * @param vectorizer The vectorizer to use for finding relevant context
     * @param textChunks The text chunks from the document
     * @param userLevel The user's skill level
     */
    public ChatPanel(GeminiClient geminiClient, TfidfVectorizer vectorizer, List<String> textChunks, String userLevel) {
        this.geminiClient = geminiClient;
        this.vectorizer = vectorizer;
        this.textChunks = textChunks;
        this.userLevel = userLevel;
        
        // Set up the layout
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create the chat display
        chatDisplay = new JTextArea();
        chatDisplay.setEditable(false);
        chatDisplay.setLineWrap(true);
        chatDisplay.setWrapStyleWord(true);
        chatDisplay.setFont(new Font("Sans-Serif", Font.PLAIN, 14));
        
        // Add a welcome message based on the user's level
        displayWelcomeMessage();
        
        // Create the scroll pane
        scrollPane = new JScrollPane(chatDisplay);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // Create the user input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        
        userInput = new JTextField();
        userInput.addActionListener(this::sendMessage);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        
        inputPanel.add(userInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        // Add everything to the main panel
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Displays a welcome message based on the user's skill level
     */
    private void displayWelcomeMessage() {
        String welcomeMessage = "Welcome to LearnIQ Chat!\n\n";
        
        switch (userLevel) {
            case "beginner":
                welcomeMessage += "I notice you're new to this topic. I'll provide simple explanations with examples.\n" +
                                 "Feel free to ask any questions about the document you've uploaded.";
                break;
            case "intermediate":
                welcomeMessage += "You have a good foundation on this topic. I'll provide balanced explanations with some technical details.\n" +
                                 "Ask me anything about the document you've uploaded.";
                break;
            case "advanced":
                welcomeMessage += "You have advanced knowledge in this topic. I'll provide concise, in-depth responses.\n" +
                                 "Ask me challenging questions about the document you've uploaded.";
                break;
            default:
                welcomeMessage += "Ask me anything about the document you've uploaded.";
        }
        
        chatDisplay.append("LearnIQ AI: " + welcomeMessage + "\n\n");
    }
    
    /**
     * Sends a message to the AI and displays the response
     * 
     * @param e The action event
     */
    private void sendMessage(ActionEvent e) {
        String message = userInput.getText().trim();
        
        if (message.isEmpty()) {
            return;
        }
        
        // Display the user's message
        chatDisplay.append("You: " + message + "\n\n");
        
        // Clear the input field
        userInput.setText("");
        
        // Disable the send button and input field while processing
        sendButton.setEnabled(false);
        userInput.setEnabled(false);
        
        // Find relevant context
        List<String> relevantChunks = vectorizer.getRelevantChunks(message, textChunks, 3);
        String context = String.join("\n\n", relevantChunks);
        
        // Generate a response in a background thread
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    return geminiClient.generateAdaptiveResponse(message, context, userLevel);
                } catch (IOException ex) {
                    return "Sorry, I encountered an error while generating a response: " + ex.getMessage();
                }
            }
            
            @Override
            protected void done() {
                try {
                    String response = get();
                    chatDisplay.append("LearnIQ AI: " + response + "\n\n");
                    
                    // Scroll to the bottom of the chat
                    chatDisplay.setCaretPosition(chatDisplay.getDocument().getLength());
                } catch (Exception ex) {
                    chatDisplay.append("LearnIQ AI: Sorry, I encountered an error: " + ex.getMessage() + "\n\n");
                }
                
                // Re-enable the send button and input field
                sendButton.setEnabled(true);
                userInput.setEnabled(true);
                userInput.requestFocus();
            }
        };
        
        worker.execute();
    }
}