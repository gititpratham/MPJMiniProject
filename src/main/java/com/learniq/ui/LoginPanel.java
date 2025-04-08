package com.learniq.ui;

import com.learniq.db.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for user login
 */
public class LoginPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(LoginPanel.class.getName());
    
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton signupButton;
    private final JLabel statusLabel;
    
    private final LoginListener loginListener;
    
    /**
     * Creates a new login panel
     * 
     * @param loginListener The listener for login events
     */
    public LoginPanel(LoginListener loginListener) {
        this.loginListener = loginListener;
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create the form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Add the title
        JLabel titleLabel = new JLabel("Login to LearnIQ");
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);
        
        // Add the email field
        JLabel emailLabel = new JLabel("Email:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        formPanel.add(emailLabel, gbc);
        
        emailField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(emailField, gbc);
        
        // Add the password field
        JLabel passwordLabel = new JLabel("Password:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(passwordLabel, gbc);
        
        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(passwordField, gbc);
        
        // Add the status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(statusLabel, gbc);
        
        // Add the login button
        loginButton = new JButton("Login");
        loginButton.addActionListener(e -> handleLogin());
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(loginButton, gbc);
        
        // Add the signup button
        signupButton = new JButton("Sign Up");
        signupButton.addActionListener(e -> loginListener.onSignupRequested());
        gbc.gridx = 1;
        gbc.gridy = 4;
        formPanel.add(signupButton, gbc);
        
        // Add the form panel to the center
        add(formPanel, BorderLayout.CENTER);
    }
    
    /**
     * Handles the login button click
     */
    private void handleLogin() {
        // Clear the status label
        statusLabel.setText(" ");
        
        // Get the email and password
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Validate the inputs
        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter your email and password");
            return;
        }
        
        // Disable the buttons
        loginButton.setEnabled(false);
        signupButton.setEnabled(false);
        
        // Start a background thread to handle the login
        new Thread(() -> {
            try {
                // Authenticate the user
                User user = User.authenticate(email, password);
                
                // Check if the authentication was successful
                if (user != null) {
                    // Notify the listener
                    SwingUtilities.invokeLater(() -> loginListener.onLoginSuccessful(user));
                } else {
                    // Show an error message
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Invalid email or password");
                        loginButton.setEnabled(true);
                        signupButton.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during login: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error during login: " + e.getMessage());
                    loginButton.setEnabled(true);
                    signupButton.setEnabled(true);
                });
            }
        }).start();
    }
    
    /**
     * Clears the login form
     */
    public void clear() {
        emailField.setText("");
        passwordField.setText("");
        statusLabel.setText(" ");
        loginButton.setEnabled(true);
        signupButton.setEnabled(true);
    }
    
    /**
     * Interface for login events
     */
    public interface LoginListener {
        /**
         * Called when the login is successful
         * 
         * @param user The authenticated user
         */
        void onLoginSuccessful(User user);
        
        /**
         * Called when the signup button is clicked
         */
        void onSignupRequested();
    }
}