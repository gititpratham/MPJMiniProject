package com.learniq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates quizzes and evaluates user responses
 */
public class QuizGenerator {
    private final GeminiClient geminiClient;
    
    /**
     * Creates a new QuizGenerator
     * 
     * @param geminiClient The Gemini client to use for generating quizzes
     */
    public QuizGenerator(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }
    
    /**
     * Generates a quiz from the provided text chunks
     * 
     * @param chunks The text chunks to generate questions from
     * @return A list of quiz questions
     * @throws IOException If the API request fails
     */
    public List<QuizQuestion> generateQuiz(List<String> chunks) throws IOException {
        // Select a random sample of chunks to use for the quiz
        List<String> sampleChunks = getRandomSample(chunks, Math.min(3, chunks.size()));
        
        // Join the chunks together
        String context = String.join("\n\n", sampleChunks);
        
        // Generate the quiz text
        String quizText = geminiClient.generateQuiz(context);
        
        // Parse the quiz text into questions
        return parseQuestions(quizText);
    }
    
    /**
     * Evaluates the user's quiz results to determine their knowledge level
     * 
     * @param quizResults A list of boolean values indicating correct (true) or incorrect (false) answers
     * @return The user's knowledge level (beginner, intermediate, or advanced)
     */
    public String evaluateUserLevel(List<Boolean> quizResults) {
        // Count the number of correct answers
        int correctAnswers = 0;
        for (Boolean result : quizResults) {
            if (result) {
                correctAnswers++;
            }
        }
        
        // Determine the user's level based on the number of correct answers
        if (correctAnswers <= 2) {
            return "beginner";
        } else if (correctAnswers <= 4) {
            return "intermediate";
        } else {
            return "advanced";
        }
    }
    
    /**
     * Parses quiz text into a list of structured question objects
     * 
     * @param quizText The text of the quiz
     * @return A list of quiz questions
     */
    private List<QuizQuestion> parseQuestions(String quizText) {
        List<QuizQuestion> questions = new ArrayList<>();
        
        // Regular expression to match questions in the expected format
        Pattern questionPattern = Pattern.compile("\\d+\\.\\s+\\[(BEGINNER|INTERMEDIATE|ADVANCED)\\]\\s+(.+?)\\s*\\n(A\\)\\s+.+?\\n)(B\\)\\s+.+?\\n)(C\\)\\s+.+?\\n)(D\\)\\s+.+?)\\s*\\nCorrect:\\s+([A-D])", Pattern.DOTALL);
        
        Matcher matcher = questionPattern.matcher(quizText);
        
        while (matcher.find()) {
            try {
                String difficultyLevel = matcher.group(1).toLowerCase();
                String questionText = matcher.group(2);
                String optionA = matcher.group(3).trim().substring(3).trim();
                String optionB = matcher.group(4).trim().substring(3).trim();
                String optionC = matcher.group(5).trim().substring(3).trim();
                String optionD = matcher.group(6).trim().substring(3).trim();
                String correctAnswer = matcher.group(7);
                
                // Create the question object
                QuizQuestion question = new QuizQuestion(
                    questionText,
                    new String[] { optionA, optionB, optionC, optionD },
                    correctAnswer,
                    difficultyLevel
                );
                
                questions.add(question);
            } catch (Exception e) {
                System.err.println("Error parsing question: " + e.getMessage());
            }
        }
        
        return questions;
    }
    
    /**
     * Gets a random sample of elements from a list
     * 
     * @param list The list to sample from
     * @param sampleSize The number of elements to sample
     * @return A new list containing the sampled elements
     */
    private <T> List<T> getRandomSample(List<T> list, int sampleSize) {
        if (list.size() <= sampleSize) {
            return new ArrayList<>(list);
        }
        
        List<T> sample = new ArrayList<>();
        List<T> tempList = new ArrayList<>(list);
        Random rand = new Random();
        
        for (int i = 0; i < sampleSize; i++) {
            int index = rand.nextInt(tempList.size());
            sample.add(tempList.get(index));
            tempList.remove(index);
        }
        
        return sample;
    }
    
    /**
     * Represents a quiz question with options and metadata
     */
    public static class QuizQuestion {
        private final String question;
        private final String[] options;
        private final String correctAnswer;
        private final String difficultyLevel;
        
        /**
         * Creates a new QuizQuestion
         * 
         * @param question The question text
         * @param options The answer options
         * @param correctAnswer The correct answer (A, B, C, or D)
         * @param difficultyLevel The difficulty level (beginner, intermediate, or advanced)
         */
        public QuizQuestion(String question, String[] options, String correctAnswer, String difficultyLevel) {
            this.question = question;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.difficultyLevel = difficultyLevel;
        }
        
        /**
         * Gets the question text
         * 
         * @return The question text
         */
        public String getQuestion() {
            return question;
        }
        
        /**
         * Gets the answer options
         * 
         * @return The answer options
         */
        public String[] getOptions() {
            return options;
        }
        
        /**
         * Gets the correct answer (A, B, C, or D)
         * 
         * @return The correct answer
         */
        public String getCorrectAnswer() {
            return correctAnswer;
        }
        
        /**
         * Gets the difficulty level
         * 
         * @return The difficulty level (beginner, intermediate, or advanced)
         */
        public String getDifficultyLevel() {
            return difficultyLevel;
        }
        
        /**
         * Checks if the given answer is correct
         * 
         * @param answer The answer to check (A, B, C, or D)
         * @return True if the answer is correct, false otherwise
         */
        public boolean isCorrect(String answer) {
            return correctAnswer.equalsIgnoreCase(answer);
        }
    }
}