package com.learniq.ui;

import com.learniq.db.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for user signup
 */
public class SignupPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(SignupPanel.class.getName());
    
    private final JTextField emailField;
    private final JTextField fullNameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final JButton signupButton;
    private final JButton backButton;
    private final JLabel statusLabel;
    
    private final SignupListener signupListener;
    
    /**
     * Creates a new signup panel
     * 
     * @param signupListener The listener for signup events
     */
    public SignupPanel(SignupListener signupListener) {
        this.signupListener = signupListener;
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create the form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add the title
        JLabel titleLabel = new JLabel("Create a LearnIQ Account");
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);
        
        // Add the full name field
        JLabel fullNameLabel = new JLabel("Full Name:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(fullNameLabel, gbc);
        
        fullNameField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(fullNameField, gbc);
        
        // Add the email field
        JLabel emailLabel = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(emailLabel, gbc);
        
        emailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(emailField, gbc);
        
        // Add the password field
        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(passwordLabel, gbc);
        
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(passwordField, gbc);
        
        // Add the confirm password field
        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(confirmPasswordLabel, gbc);
        
        confirmPasswordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        formPanel.add(confirmPasswordField, gbc);
        
        // Add the status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        formPanel.add(statusLabel, gbc);
        
        // Add the signup button
        signupButton = new JButton("Sign Up");
        signupButton.addActionListener(e -> handleSignup());
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        formPanel.add(signupButton, gbc);
        
        // Add the back button
        backButton = new JButton("Back to Login");
        backButton.addActionListener(e -> signupListener.onBackToLoginRequested());
        gbc.gridx = 1;
        gbc.gridy = 6;
        formPanel.add(backButton, gbc);
        
        // Add the form panel to the center
        add(formPanel, BorderLayout.CENTER);
    }
    
    /**
     * Handles the signup button click
     */
    private void handleSignup() {
        // Clear the status label
        statusLabel.setText(" ");
        
        // Get the user input
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Validate the inputs
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("Passwords do not match");
            return;
        }
        
        if (password.length() < 6) {
            statusLabel.setText("Password must be at least 6 characters");
            return;
        }
        
        if (!email.contains("@") || !email.contains(".")) {
            statusLabel.setText("Please enter a valid email address");
            return;
        }
        
        // Disable the buttons
        signupButton.setEnabled(false);
        backButton.setEnabled(false);
        
        // Start a background thread to handle the signup
        new Thread(() -> {
            try {
                // Check if the email already exists
                if (User.emailExists(email)) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Email address already in use");
                        signupButton.setEnabled(true);
                        backButton.setEnabled(true);
                    });
                    return;
                }
                
                // Create a new user
                User user = new User(email, fullName, password);
                
                // Save the user
                if (user.save()) {
                    // Notify the listener
                    SwingUtilities.invokeLater(() -> signupListener.onSignupSuccessful(user));
                } else {
                    // Show an error message
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error creating account");
                        signupButton.setEnabled(true);
                        backButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during signup: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error during signup: " + e.getMessage());
                    signupButton.setEnabled(true);
                    backButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    /**
     * Clears the signup form
     */
    public void clear() {
        fullNameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        statusLabel.setText(" ");
        signupButton.setEnabled(true);
        backButton.setEnabled(true);
    }
    
    /**
     * Interface for signup events
     */
    public interface SignupListener {
        /**
         * Called when the signup is successful
         * 
         * @param user The newly created user
         */
        void onSignupSuccessful(User user);
        
        /**
         * Called when the back button is clicked
         */
        void onBackToLoginRequested();
    }
}