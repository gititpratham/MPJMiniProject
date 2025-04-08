package com.learniq;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel for displaying and taking the knowledge assessment quiz
 */
public class QuizPanel extends JPanel {
    private final List<QuizGenerator.QuizQuestion> questions;
    
    private final JLabel questionLabel;
    private final JLabel questionCountLabel;
    private final JLabel difficultyLabel;
    private final JRadioButton[] optionButtons;
    private final ButtonGroup optionGroup;
    private final JButton nextButton;
    
    private int currentQuestionIndex = 0;
    private final List<Boolean> quizResults = new ArrayList<>();
    private final Consumer<String> onQuizComplete;
    
    /**
     * Creates a new quiz panel
     * 
     * @param questions The quiz questions to display
     * @param onQuizComplete Callback for when the quiz is complete
     */
    public QuizPanel(List<QuizGenerator.QuizQuestion> questions, Consumer<String> onQuizComplete) {
        this.questions = questions;
        this.onQuizComplete = onQuizComplete;
        
        // Set up the layout
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create the header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        
        // Create the question count label
        questionCountLabel = new JLabel();
        questionCountLabel.setFont(new Font("Sans-Serif", Font.BOLD, 14));
        headerPanel.add(questionCountLabel, BorderLayout.WEST);
        
        // Create the difficulty label
        difficultyLabel = new JLabel();
        difficultyLabel.setFont(new Font("Sans-Serif", Font.ITALIC, 14));
        headerPanel.add(difficultyLabel, BorderLayout.EAST);
        
        // Create the question panel
        JPanel questionPanel = new JPanel(new BorderLayout(0, 15));
        questionPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Create the question label
        questionLabel = new JLabel();
        questionLabel.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        questionPanel.add(questionLabel, BorderLayout.NORTH);
        
        // Create the options panel
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 0, 10));
        
        // Create the option buttons
        optionButtons = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Sans-Serif", Font.PLAIN, 14));
            optionGroup.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        
        questionPanel.add(optionsPanel, BorderLayout.CENTER);
        
        // Create the next button
        nextButton = new JButton("Next Question");
        nextButton.addActionListener(this::nextQuestion);
        
        // Add everything to the main panel
        add(headerPanel, BorderLayout.NORTH);
        add(questionPanel, BorderLayout.CENTER);
        add(nextButton, BorderLayout.SOUTH);
        
        // Show the first question
        showQuestion(0);
    }
    
    /**
     * Shows the specified question
     * 
     * @param index The index of the question to show
     */
    private void showQuestion(int index) {
        if (index >= questions.size()) {
            return;
        }
        
        QuizGenerator.QuizQuestion question = questions.get(index);
        
        // Update the question count label
        questionCountLabel.setText("Question " + (index + 1) + " of " + questions.size());
        
        // Update the difficulty label
        String difficulty = question.getDifficultyLevel().toUpperCase();
        difficultyLabel.setText("Level: " + difficulty);
        
        // Update the question label
        questionLabel.setText("<html><body style='width: 400px'>" + question.getQuestion() + "</body></html>");
        
        // Update the option buttons
        String[] options = question.getOptions();
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText((char)('A' + i) + ") " + options[i]);
            optionButtons[i].setSelected(false);
        }
        
        // Update the next button
        nextButton.setEnabled(false);
        
        // Add action listeners to enable the next button when an option is selected
        for (JRadioButton button : optionButtons) {
            button.addActionListener(e -> nextButton.setEnabled(true));
        }
    }
    
    /**
     * Handles the next question button
     * 
     * @param e The action event
     */
    private void nextQuestion(ActionEvent e) {
        // Get the selected option
        int selectedOption = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i].isSelected()) {
                selectedOption = i;
                break;
            }
        }
        
        if (selectedOption == -1) {
            return;
        }
        
        // Check if the answer is correct
        QuizGenerator.QuizQuestion question = questions.get(currentQuestionIndex);
        String selectedAnswer = String.valueOf((char)('A' + selectedOption));
        boolean isCorrect = question.isCorrect(selectedAnswer);
        
        // Add the result to the quiz results
        quizResults.add(isCorrect);
        
        // Show a message
        if (isCorrect) {
            JOptionPane.showMessageDialog(this, "Correct!", "Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect. The correct answer is " + question.getCorrectAnswer() + ".", 
                "Result", JOptionPane.ERROR_MESSAGE);
        }
        
        // Move to the next question or finish the quiz
        currentQuestionIndex++;
        
        if (currentQuestionIndex < questions.size()) {
            // Show the next question
            showQuestion(currentQuestionIndex);
        } else {
            // Finish the quiz
            finishQuiz();
        }
    }
    
    /**
     * Finishes the quiz and calculates the user's level
     */
    private void finishQuiz() {
        // Calculate the user's level
        int correctAnswers = 0;
        for (Boolean result : quizResults) {
            if (result) {
                correctAnswers++;
            }
        }
        
        String userLevel;
        if (correctAnswers <= 2) {
            userLevel = "beginner";
        } else if (correctAnswers <= 4) {
            userLevel = "intermediate";
        } else {
            userLevel = "advanced";
        }
        
        // Show a congratulations message
        String message = "Quiz completed!\n\n" +
            "You answered " + correctAnswers + " out of " + questions.size() + " questions correctly.\n" +
            "Your knowledge level: " + userLevel.toUpperCase();
        
        JOptionPane.showMessageDialog(this, message, "Quiz Completed", JOptionPane.INFORMATION_MESSAGE);
        
        // Call the onQuizComplete callback
        onQuizComplete.accept(userLevel);
    }
}