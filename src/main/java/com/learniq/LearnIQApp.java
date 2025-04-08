package com.learniq;

import com.learniq.db.DbUtils;
import com.learniq.db.Document;
import com.learniq.db.User;
import com.learniq.ui.DashboardPanel;
import com.learniq.ui.LoginPanel;
import com.learniq.ui.SignupPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application class for LearnIQ
 */
public class LearnIQApp extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(LearnIQApp.class.getName());
    
    private final PdfProcessor pdfProcessor = new PdfProcessor();
    private final TfidfVectorizer vectorizer = new TfidfVectorizer();
    private GeminiClient geminiClient;
    private QuizGenerator quizGenerator;
    
    private List<String> textChunks;
    private String userLevel;
    
    private User currentUser;
    private Document currentDocument;
    
    private final JPanel mainPanel;
    private final CardLayout cardLayout;
    
    private final JPanel welcomePanel;
    private final JPanel loadingPanel;
    private LoginPanel loginPanel;
    private SignupPanel signupPanel;
    private DashboardPanel dashboardPanel;
    
    /**
     * Creates a new LearnIQ application
     */
    public LearnIQApp() {
        // Set up the frame
        setTitle("LearnIQ - Interactive PDF Learning Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);
        
        // Initialize the Gemini client with the API key from environment
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please set the GEMINI_API_KEY environment variable to use this application.",
                "API Key Missing",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        geminiClient = new GeminiClient(apiKey);
        quizGenerator = new QuizGenerator(geminiClient);
        
        // Set up the main panel with a card layout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create the welcome panel
        welcomePanel = createWelcomePanel();
        
        // Create the loading panel
        loadingPanel = createLoadingPanel();
        
        // Check if Neo4j environment variables are set
        if (System.getenv("NEO4J_URI") == null || 
            System.getenv("NEO4J_USERNAME") == null || 
            System.getenv("NEO4J_PASSWORD") == null) {
            
            // Just proceed with the local version
            LOGGER.info("Neo4j environment variables not found. Running in local mode.");
            
            // Add the panels to the card layout
            mainPanel.add(welcomePanel, "welcome");
            mainPanel.add(loadingPanel, "loading");
            
            // Start with the welcome panel
            cardLayout.show(mainPanel, "welcome");
        } else {
            // Initialize the Neo4j database
            try {
                // Create the documents folder if it doesn't exist
                DbUtils.ensureDocumentsFolderExists();
                
                // Create the authentication panels
                loginPanel = new LoginPanel(new LoginPanel.LoginListener() {
                    @Override
                    public void onLoginSuccessful(User user) {
                        showDashboard(user);
                    }
                    
                    @Override
                    public void onSignupRequested() {
                        showSignupPanel();
                    }
                });
                
                signupPanel = new SignupPanel(new SignupPanel.SignupListener() {
                    @Override
                    public void onSignupSuccessful(User user) {
                        showDashboard(user);
                    }
                    
                    @Override
                    public void onBackToLoginRequested() {
                        showLoginPanel();
                    }
                });
                
                // Add the panels to the card layout
                mainPanel.add(loginPanel, "login");
                mainPanel.add(signupPanel, "signup");
                mainPanel.add(welcomePanel, "welcome");
                mainPanel.add(loadingPanel, "loading");
                
                // Start with the login panel
                cardLayout.show(mainPanel, "login");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error initializing database: " + e.getMessage(), e);
                
                // Fall back to local mode
                JOptionPane.showMessageDialog(this,
                    "Error connecting to the database. Running in local mode.",
                    "Database Error",
                    JOptionPane.WARNING_MESSAGE);
                
                // Add the panels to the card layout
                mainPanel.add(welcomePanel, "welcome");
                mainPanel.add(loadingPanel, "loading");
                
                // Start with the welcome panel
                cardLayout.show(mainPanel, "welcome");
            }
        }
        
        // Add the main panel to the frame
        add(mainPanel);
    }
    
    /**
     * Creates the welcome panel
     * 
     * @return The welcome panel
     */
    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        // Create the header
        JLabel headerLabel = new JLabel("LearnIQ - Interactive PDF Learning Tool");
        headerLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
        headerLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(headerLabel, BorderLayout.NORTH);
        
        // Create the content panel
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        
        // Create the description
        JTextArea descriptionArea = new JTextArea(
            "Welcome to LearnIQ, your interactive learning companion!\n\n" +
            "Upload a PDF document to get started. LearnIQ will:\n" +
            "1. Generate a knowledge assessment quiz based on the document\n" +
            "2. Evaluate your knowledge level\n" +
            "3. Provide personalized answers to your questions\n\n" +
            "Click the button below to upload a PDF.");
        descriptionArea.setEditable(false);
        descriptionArea.setBackground(null);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Sans-Serif", Font.PLAIN, 16));
        contentPanel.add(descriptionArea, BorderLayout.CENTER);
        
        // Create the upload button
        JButton uploadButton = new JButton("Upload PDF Book");
        uploadButton.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        uploadButton.addActionListener(e -> selectPdfFile());
        
        // Center the button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(uploadButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the loading panel
     * 
     * @return The loading panel
     */
    private JPanel createLoadingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        JLabel loadingLabel = new JLabel("Processing your document...");
        loadingLabel.setFont(new Font("Sans-Serif", Font.PLAIN, 18));
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        panel.add(loadingLabel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Opens a file chooser to select a PDF file
     */
    private void selectPdfFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDF Book");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf", "txt"));
        
        int result = fileChooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processPdfFile(selectedFile);
        }
    }
    
    /**
     * Processes the selected PDF file
     * 
     * @param pdfFile The PDF file to process
     */
    private void processPdfFile(File pdfFile) {
        // Show the loading panel
        cardLayout.show(mainPanel, "loading");
        
        // Process the file in a background thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Extract text from the PDF
                    String text = pdfProcessor.extractText(pdfFile);
                    
                    // Split the text into chunks
                    textChunks = pdfProcessor.splitIntoChunks(text, 1000);
                    
                    // Generate and show the quiz
                    SwingUtilities.invokeLater(() -> showQuiz());
                } catch (IOException e) {
                    // Show an error message
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(LearnIQApp.this,
                            "Error processing the file: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        cardLayout.show(mainPanel, "welcome");
                    });
                }
                
                return null;
            }
        };
        
        worker.execute();
    }
    
    /**
     * Shows the quiz panel
     */
    private void showQuiz() {
        try {
            // Generate the quiz
            List<QuizGenerator.QuizQuestion> questions = quizGenerator.generateQuiz(textChunks);
            
            // Create the quiz panel
            QuizPanel quizPanel = new QuizPanel(questions, this::onQuizComplete);
            
            // Add the quiz panel to the card layout
            mainPanel.add(quizPanel, "quiz");
            cardLayout.show(mainPanel, "quiz");
        } catch (IOException e) {
            // Show an error message
            JOptionPane.showMessageDialog(this,
                "Error generating the quiz: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
            cardLayout.show(mainPanel, "welcome");
        }
    }
    
    /**
     * Called when the quiz is complete
     * 
     * @param userLevel The user's knowledge level
     */
    private void onQuizComplete(String userLevel) {
        this.userLevel = userLevel;
        
        // Create the chat panel
        ChatPanel chatPanel = new ChatPanel(geminiClient, vectorizer, textChunks, userLevel);
        
        // Add the chat panel to the card layout
        mainPanel.add(chatPanel, "chat");
        cardLayout.show(mainPanel, "chat");
    }
    
    /**
     * Shows the login panel
     */
    private void showLoginPanel() {
        loginPanel.clear();
        cardLayout.show(mainPanel, "login");
    }
    
    /**
     * Shows the signup panel
     */
    private void showSignupPanel() {
        signupPanel.clear();
        cardLayout.show(mainPanel, "signup");
    }
    
    /**
     * Shows the dashboard panel for the current user
     * 
     * @param user The current user
     */
    private void showDashboard(User user) {
        this.currentUser = user;
        
        // Create or update the dashboard panel
        if (dashboardPanel == null) {
            dashboardPanel = new DashboardPanel(user, new DashboardPanel.DashboardListener() {
                @Override
                public void onDocumentSelected(Document document) {
                    openDocument(document);
                }
                
                @Override
                public void onLogoutRequested() {
                    logout();
                }
            });
            
            mainPanel.add(dashboardPanel, "dashboard");
        } else {
            // Remove the old dashboard
            mainPanel.remove(dashboardPanel);
            
            // Create a new dashboard
            dashboardPanel = new DashboardPanel(user, new DashboardPanel.DashboardListener() {
                @Override
                public void onDocumentSelected(Document document) {
                    openDocument(document);
                }
                
                @Override
                public void onLogoutRequested() {
                    logout();
                }
            });
            
            // Add the new dashboard
            mainPanel.add(dashboardPanel, "dashboard");
        }
        
        // Show the dashboard
        cardLayout.show(mainPanel, "dashboard");
    }
    
    /**
     * Opens a document for processing
     * 
     * @param document The document to open
     */
    private void openDocument(Document document) {
        // Set the current document
        this.currentDocument = document;
        
        // Show the loading panel
        cardLayout.show(mainPanel, "loading");
        
        // Process the document in a background thread
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    // Load the file
                    File file = new File(document.getFilePath());
                    
                    // Extract text from the PDF
                    String text = pdfProcessor.extractText(file);
                    
                    // Split the text into chunks
                    textChunks = pdfProcessor.splitIntoChunks(text, 1000);
                    
                    // Generate and show the quiz
                    SwingUtilities.invokeLater(() -> showQuiz());
                } catch (IOException e) {
                    // Show an error message
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(LearnIQApp.this,
                            "Error processing the document: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        cardLayout.show(mainPanel, "dashboard");
                    });
                }
                
                return null;
            }
        };
        
        worker.execute();
    }
    
    /**
     * Logs out the current user
     */
    private void logout() {
        // Clear the user and document data
        currentUser = null;
        currentDocument = null;
        textChunks = null;
        userLevel = null;
        
        // Show the login panel
        showLoginPanel();
    }
    
    /**
     * Main entry point for the application
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Set the look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Start the application
        SwingUtilities.invokeLater(() -> {
            LearnIQApp app = new LearnIQApp();
            app.setVisible(true);
        });
    }
}