package com.learniq.ui;

import com.learniq.db.DbUtils;
import com.learniq.db.Document;
import com.learniq.db.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for user dashboard
 */
public class DashboardPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(DashboardPanel.class.getName());
    
    private final User currentUser;
    private final DashboardListener dashboardListener;
    
    private final JLabel welcomeLabel;
    private final JTable documentsTable;
    private final DefaultTableModel tableModel;
    private final JButton addDocumentButton;
    private final JButton openDocumentButton;
    private final JButton deleteDocumentButton;
    private final JButton logoutButton;
    private final JLabel statusLabel;
    
    /**
     * Creates a new dashboard panel
     * 
     * @param user The current user
     * @param dashboardListener The listener for dashboard events
     */
    public DashboardPanel(User user, DashboardListener dashboardListener) {
        this.currentUser = user;
        this.dashboardListener = dashboardListener;
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create the header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Add the welcome label
        welcomeLabel = new JLabel("Welcome, " + user.getFullName());
        welcomeLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        
        // Add the logout button
        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> dashboardListener.onLogoutRequested());
        headerPanel.add(logoutButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        
        // Create the table model
        tableModel = new DefaultTableModel(
            new Object[]{"Title", "Created Date", "Last Accessed"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Create the documents table
        documentsTable = new JTable(tableModel);
        documentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        documentsTable.getTableHeader().setReorderingAllowed(false);
        documentsTable.setRowHeight(25);
        
        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(documentsTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create the buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add the buttons
        addDocumentButton = new JButton("Add Document");
        addDocumentButton.addActionListener(e -> handleAddDocument());
        buttonsPanel.add(addDocumentButton);
        
        openDocumentButton = new JButton("Open Document");
        openDocumentButton.addActionListener(e -> handleOpenDocument());
        openDocumentButton.setEnabled(false);
        buttonsPanel.add(openDocumentButton);
        
        deleteDocumentButton = new JButton("Delete Document");
        deleteDocumentButton.addActionListener(e -> handleDeleteDocument());
        deleteDocumentButton.setEnabled(false);
        buttonsPanel.add(deleteDocumentButton);
        
        // Add the status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        buttonsPanel.add(statusLabel);
        
        add(buttonsPanel, BorderLayout.SOUTH);
        
        // Add selection listener to the table
        documentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = documentsTable.getSelectedRow() != -1;
                openDocumentButton.setEnabled(hasSelection);
                deleteDocumentButton.setEnabled(hasSelection);
            }
        });
        
        // Load the documents
        loadDocuments();
    }
    
    /**
     * Loads the user's documents
     */
    public void loadDocuments() {
        // Clear the table
        tableModel.setRowCount(0);
        
        // Disable the buttons
        openDocumentButton.setEnabled(false);
        deleteDocumentButton.setEnabled(false);
        
        // Start a background thread to load the documents
        new Thread(() -> {
            try {
                // Get the documents
                List<Document> documents = Document.getByUserId(currentUser.getId());
                
                // Add the documents to the table
                for (Document document : documents) {
                    SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                        document.getTitle(),
                        DbUtils.formatDate(document.getCreatedAt()),
                        DbUtils.formatDate(document.getLastAccessed())
                    }));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading documents: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> 
                    statusLabel.setText("Error loading documents: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Handles the add document button click
     */
    private void handleAddDocument() {
        // Create a file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PDF File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        
        // Show the file chooser
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        // Get the selected file
        File selectedFile = fileChooser.getSelectedFile();
        
        // Check if the file is a PDF
        if (!selectedFile.getName().toLowerCase().endsWith(".pdf")) {
            statusLabel.setText("Please select a PDF file");
            return;
        }
        
        // Disable the buttons
        addDocumentButton.setEnabled(false);
        
        // Start a background thread to add the document
        new Thread(() -> {
            try {
                // Save the document
                String documentId = DbUtils.saveUserDocument(
                    selectedFile.getAbsolutePath(),
                    selectedFile.getName(),
                    currentUser.getId()
                );
                
                if (documentId != null) {
                    // Reload the documents
                    SwingUtilities.invokeLater(() -> {
                        loadDocuments();
                        addDocumentButton.setEnabled(true);
                        statusLabel.setText("Document added successfully");
                    });
                } else {
                    // Show an error message
                    SwingUtilities.invokeLater(() -> {
                        addDocumentButton.setEnabled(true);
                        statusLabel.setText("Error adding document");
                    });
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error adding document: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> {
                    addDocumentButton.setEnabled(true);
                    statusLabel.setText("Error adding document: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Handles the open document button click
     */
    private void handleOpenDocument() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        // Get the document title
        String title = (String) tableModel.getValueAt(selectedRow, 0);
        
        // Start a background thread to get the document
        new Thread(() -> {
            try {
                // Get the documents
                List<Document> documents = Document.getByUserId(currentUser.getId());
                
                // Find the document with the matching title
                for (Document document : documents) {
                    if (document.getTitle().equals(title)) {
                        // Notify the listener
                        SwingUtilities.invokeLater(() -> 
                            dashboardListener.onDocumentSelected(document));
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error opening document: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> 
                    statusLabel.setText("Error opening document: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Handles the delete document button click
     */
    private void handleDeleteDocument() {
        int selectedRow = documentsTable.getSelectedRow();
        if (selectedRow == -1) {
            return;
        }
        
        // Get the document title
        String title = (String) tableModel.getValueAt(selectedRow, 0);
        
        // Show a confirmation dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the document \"" + title + "\"?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
        // Disable the buttons
        openDocumentButton.setEnabled(false);
        deleteDocumentButton.setEnabled(false);
        
        // Start a background thread to delete the document
        new Thread(() -> {
            try {
                // Get the documents
                List<Document> documents = Document.getByUserId(currentUser.getId());
                
                // Find the document with the matching title
                for (Document document : documents) {
                    if (document.getTitle().equals(title)) {
                        // Delete the document
                        if (DbUtils.deleteUserDocument(document.getId())) {
                            // Reload the documents
                            SwingUtilities.invokeLater(() -> {
                                loadDocuments();
                                statusLabel.setText("Document deleted successfully");
                            });
                        } else {
                            // Show an error message
                            SwingUtilities.invokeLater(() -> {
                                openDocumentButton.setEnabled(true);
                                deleteDocumentButton.setEnabled(true);
                                statusLabel.setText("Error deleting document");
                            });
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting document: " + e.getMessage(), e);
                
                // Show an error message
                SwingUtilities.invokeLater(() -> {
                    openDocumentButton.setEnabled(true);
                    deleteDocumentButton.setEnabled(true);
                    statusLabel.setText("Error deleting document: " + e.getMessage());
                });
            }
        }).start();
    }
    
    /**
     * Interface for dashboard events
     */
    public interface DashboardListener {
        /**
         * Called when the user selects a document
         * 
         * @param document The selected document
         */
        void onDocumentSelected(Document document);
        
        /**
         * Called when the user requests to logout
         */
        void onLogoutRequested();
    }
}