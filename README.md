# LearnIQ - Interactive PDF Learning Tool

LearnIQ is a Java desktop application that processes PDFs, generates quizzes, and provides intelligent AI responses using Google's Gemini API. The application helps users learn and understand content from PDF books by taking a knowledge assessment quiz and then providing personalized responses based on the user's skill level.

## Features

- **PDF Processing:** Extract text from PDF documents
- **Quiz Generation:** AI-powered quiz generation based on document content
- **Adaptive Learning:** Personalized responses based on assessed knowledge level
- **Contextual Answers:** Get answers to questions with relevant context from the document

## Requirements

- Java 11 or higher
- Google Gemini API key (get one from https://makersuite.google.com/app/apikey)
- Access to Gemini 1.5 Pro model (the application uses the "models/gemini-1.5-pro" model)

## Setup and Installation

### Option 1: Run from pre-built JAR (Recommended)

1. Download the latest release JAR file from the releases page
2. Set up your Gemini API key as an environment variable:
   ```
   export GEMINI_API_KEY=your_api_key_here
   ```
   (On Windows: `set GEMINI_API_KEY=your_api_key_here`)
3. Run the application:
   ```
   java -jar learniq-app-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

### Option 2: Build from source

1. Ensure JDK 11+ and Maven are installed
2. Clone the repository
3. Navigate to the project directory
4. Build the project:
   ```
   mvn clean package assembly:single
   ```
5. Set up your Gemini API key as an environment variable:
   ```
   export GEMINI_API_KEY=your_api_key_here
   ```
   (On Windows: `set GEMINI_API_KEY=your_api_key_here`)
6. Run the application:
   ```
   java -jar target/learniq-app-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

### Option 3: Run with the script

1. Set up your Gemini API key as an environment variable:
   ```
   export GEMINI_API_KEY=your_api_key_here
   ```
   (On Windows: `set GEMINI_API_KEY=your_api_key_here`)
2. Run the provided script:
   ```
   chmod +x compile_and_run.sh
   ./compile_and_run.sh --run
   ```

## How to Use

1. **Upload a PDF:** Click the "Upload PDF Book" button to select a PDF document.
2. **Take the Quiz:** Answer the knowledge assessment questions to determine your skill level.
3. **Ask Questions:** Use the chat interface to ask questions about the content of the PDF.

The application will automatically adjust its responses based on your assessed knowledge level (beginner, intermediate, or advanced).

## Technical Details

- Built with Java Swing for the GUI
- Uses Google Gemini API for AI-powered responses
- Implements TF-IDF for context-relevant information retrieval
- Processes PDFs using Apache PDFBox

## Limitations in Development Environment

- When running in the development environment, the application may be limited to processing text files (TXT) instead of PDF files, which is a temporary limitation until the PDFBox library is properly configured.
- For testing, you can convert PDFs to TXT format or use text files directly.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Thanks to Google for providing the Gemini API
- Inspired by the need for personalized learning tools