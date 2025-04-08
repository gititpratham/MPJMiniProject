package com.learniq;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Processes PDF files to extract text and split it into manageable chunks.
 * Uses Apache PDFBox library for PDF processing.
 */
public class PdfProcessor {
    
    /**
     * Extracts text from a PDF file
     * 
     * @param pdfFile The PDF file to process
     * @return The extracted text
     * @throws IOException If the file cannot be read
     */
    public String extractText(File pdfFile) throws IOException {
        if (!pdfFile.exists()) {
            throw new IOException("File not found: " + pdfFile.getAbsolutePath());
        }
        
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Splits text into chunks of approximately the specified size
     * 
     * @param text The text to split
     * @param chunkSize The approximate size of each chunk
     * @return A list of text chunks
     */
    public List<String> splitIntoChunks(String text, int chunkSize) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        
        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;
        
        for (String word : words) {
            int wordLength = word.length() + 1; // +1 for the space
            currentLength += wordLength;
            currentChunk.append(word).append(" ");
            
            if (currentLength >= chunkSize) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                currentLength = 0;
            }
        }
        
        // Add the last chunk if it contains anything
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
}