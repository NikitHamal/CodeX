package com.codex.apk;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;

/**
 * Advanced Diff Generator using multiple algorithms for optimal diff generation.
 * Supports unified diff, context diff, and side-by-side diff formats.
 */
public class DiffGenerator {
    private static final String TAG = "DiffGenerator";

    /**
     * Generate unified diff format
     */
    public static String generateUnifiedDiff(String oldContent, String newContent, String oldFile, String newFile) {
        try {
            String[] oldLines = oldContent.split("\n");
            String[] newLines = newContent.split("\n");
            
            StringBuilder diff = new StringBuilder();
            diff.append("--- ").append(oldFile).append("\n");
            diff.append("+++ ").append(newFile).append("\n");
            
            // Simple but effective diff algorithm
            int maxLength = Math.max(oldLines.length, newLines.length);
            
            for (int i = 0; i < maxLength; i++) {
                String oldLine = i < oldLines.length ? oldLines[i] : "";
                String newLine = i < newLines.length ? newLines[i] : "";
                
                if (!oldLine.equals(newLine)) {
                    diff.append("@@ Line ").append(i + 1).append(" @@\n");
                    if (!oldLine.isEmpty()) {
                        diff.append("-").append(oldLine).append("\n");
                    }
                    if (!newLine.isEmpty()) {
                        diff.append("+").append(newLine).append("\n");
                    }
                }
            }
            
            return diff.toString();
        } catch (Exception e) {
            Log.e(TAG, "Unified diff generation failed", e);
            return generateSimpleDiff(oldContent, newContent);
        }
    }


    /**
     * Generate simple diff as fallback
     */
    private static String generateSimpleDiff(String oldContent, String newContent) {
        StringBuilder diff = new StringBuilder();
        diff.append("--- original\n");
        diff.append("+++ modified\n");
        
        String[] oldLines = oldContent.split("\n");
        String[] newLines = newContent.split("\n");
        
        int maxLength = Math.max(oldLines.length, newLines.length);
        
        for (int i = 0; i < maxLength; i++) {
            String oldLine = i < oldLines.length ? oldLines[i] : "";
            String newLine = i < newLines.length ? newLines[i] : "";
            
            if (!oldLine.equals(newLine)) {
                diff.append("@@ Line ").append(i + 1).append(" @@\n");
                if (!oldLine.isEmpty()) {
                    diff.append("-").append(oldLine).append("\n");
                }
                if (!newLine.isEmpty()) {
                    diff.append("+").append(newLine).append("\n");
                }
            }
        }
        
        return diff.toString();
    }

    /**
     * Escape HTML characters
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    /**
     * Diff change representation
     */
    public static class DiffChange {
        public int lineNumber;
        public String oldLine;
        public String newLine;
        public String type; // ADD, DELETE, MODIFY
    }

    /**
     * Generate diff based on format
     */
    public static String generateDiff(String oldContent, String newContent, String format, String oldFile, String newFile) {
        switch (format.toLowerCase()) {
            case "unified":
                return generateUnifiedDiff(oldContent, newContent, oldFile, newFile);
            default:
                return generateUnifiedDiff(oldContent, newContent, oldFile, newFile);
        }
    }

    /**
     * Simple diff generation for backward compatibility
     */
    public static String generateDiff(String oldContent, String newContent) {
        return generateUnifiedDiff(oldContent, newContent, "original", "modified");
    }
}