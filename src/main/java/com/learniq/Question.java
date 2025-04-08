package com.learniq;

import java.util.List;

/**
 * Represents a quiz question with multiple choice options
 */
public class Question {
    private String questionText;
    private List<String> options;
    private char correctAnswer; // A, B, C, or D
    private String difficulty; // BEGINNER, INTERMEDIATE, or ADVANCED
    
    /**
     * Creates a new Question instance
     * 
     * @param questionText The text of the question
     * @param options The answer options
     * @param correctAnswer The correct answer letter (A, B, C, or D)
     * @param difficulty The difficulty level (BEGINNER, INTERMEDIATE, or ADVANCED)
     */
    public Question(String questionText, List<String> options, char correctAnswer, String difficulty) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
    }
    
    /**
     * Gets the question text
     * 
     * @return The question text
     */
    public String getQuestionText() {
        return questionText;
    }
    
    /**
     * Gets the answer options
     * 
     * @return List of answer options
     */
    public List<String> getOptions() {
        return options;
    }
    
    /**
     * Gets the correct answer letter
     * 
     * @return The correct answer letter (A, B, C, or D)
     */
    public char getCorrectAnswer() {
        return correctAnswer;
    }
    
    /**
     * Gets the difficulty level
     * 
     * @return The difficulty level (BEGINNER, INTERMEDIATE, or ADVANCED)
     */
    public String getDifficulty() {
        return difficulty;
    }
    
    /**
     * Checks if the given answer is correct
     * 
     * @param answer The answer to check
     * @return True if the answer is correct, false otherwise
     */
    public boolean isCorrect(char answer) {
        return Character.toUpperCase(answer) == Character.toUpperCase(correctAnswer);
    }
}
