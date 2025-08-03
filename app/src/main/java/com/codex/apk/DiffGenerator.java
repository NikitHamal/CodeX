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
     * Generate context diff format
     */
    public static String generateContextDiff(String oldContent, String newContent, String oldFile, String newFile) {
        try {
            String[] oldLines = oldContent.split("\n");
            String[] newLines = newContent.split("\n");
            
            StringBuilder diff = new StringBuilder();
            diff.append("*** ").append(oldFile).append("\n");
            diff.append("--- ").append(newFile).append("\n");
            
            // Simple context diff implementation
            int maxLength = Math.max(oldLines.length, newLines.length);
            
            for (int i = 0; i < maxLength; i++) {
                String oldLine = i < oldLines.length ? oldLines[i] : "";
                String newLine = i < newLines.length ? newLines[i] : "";
                
                if (!oldLine.equals(newLine)) {
                    diff.append("! Line ").append(i + 1).append(":\n");
                    if (!oldLine.isEmpty()) {
                        diff.append("- ").append(oldLine).append("\n");
                    }
                    if (!newLine.isEmpty()) {
                        diff.append("+ ").append(newLine).append("\n");
                    }
                } else {
                    diff.append("  ").append(oldLine).append("\n");
                }
            }
            
            return diff.toString();
        } catch (Exception e) {
            Log.e(TAG, "Context diff generation failed", e);
            return generateSimpleDiff(oldContent, newContent);
        }
    }

    /**
     * Generate side-by-side diff format
     */
    public static String generateSideBySideDiff(String oldContent, String newContent) {
        try {
            String[] oldLines = oldContent.split("\n");
            String[] newLines = newContent.split("\n");
            
            StringBuilder diff = new StringBuilder();
            diff.append("OLD CONTENT").append("\t\t\t").append("NEW CONTENT\n");
            diff.append("=".repeat(50)).append("\t\t\t").append("=".repeat(50)).append("\n");
            
            int maxLength = Math.max(oldLines.length, newLines.length);
            
            for (int i = 0; i < maxLength; i++) {
                String oldLine = i < oldLines.length ? oldLines[i] : "";
                String newLine = i < newLines.length ? newLines[i] : "";
                
                if (!oldLine.equals(newLine)) {
                    diff.append("- ").append(oldLine).append("\t\t\t");
                    diff.append("+ ").append(newLine).append("\n");
                } else {
                    diff.append("  ").append(oldLine).append("\t\t\t");
                    diff.append("  ").append(newLine).append("\n");
                }
            }
            
            return diff.toString();
        } catch (Exception e) {
            Log.e(TAG, "Side-by-side diff generation failed", e);
            return generateSimpleDiff(oldContent, newContent);
        }
    }

    /**
     * Generate HTML diff for web display
     */
    public static String generateHtmlDiff(String oldContent, String newContent) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"diff-container\">\n");
            html.append("<style>\n");
            html.append(".diff-container { font-family: monospace; }\n");
            html.append(".diff-line { margin: 2px 0; }\n");
            html.append(".diff-added { background-color: #e6ffe6; color: #006600; }\n");
            html.append(".diff-removed { background-color: #ffe6e6; color: #cc0000; }\n");
            html.append(".diff-unchanged { background-color: #f9f9f9; }\n");
            html.append("</style>\n");
            
            String[] oldLines = oldContent.split("\n");
            String[] newLines = newContent.split("\n");
            
            int maxLength = Math.max(oldLines.length, newLines.length);
            
            for (int i = 0; i < maxLength; i++) {
                String oldLine = i < oldLines.length ? oldLines[i] : "";
                String newLine = i < newLines.length ? newLines[i] : "";
                
                if (!oldLine.equals(newLine)) {
                    if (!oldLine.isEmpty()) {
                        html.append("<div class=\"diff-line diff-removed\">- ").append(escapeHtml(oldLine)).append("</div>\n");
                    }
                    if (!newLine.isEmpty()) {
                        html.append("<div class=\"diff-line diff-added\">+ ").append(escapeHtml(newLine)).append("</div>\n");
                    }
                } else {
                    html.append("<div class=\"diff-line diff-unchanged\">  ").append(escapeHtml(oldLine)).append("</div>\n");
                }
            }
            
            html.append("</div>");
            return html.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTML diff generation failed", e);
            return "<div>Diff generation failed: " + e.getMessage() + "</div>";
        }
    }

    /**
     * Generate JSON diff for programmatic use
     */
    public static String generateJsonDiff(String oldContent, String newContent) {
        try {
            List<DiffChange> changes = new ArrayList<>();
            String[] oldLines = oldContent.split("\n");
            String[] newLines = newContent.split("\n");
            
            int maxLength = Math.max(oldLines.length, newLines.length);
            
            for (int i = 0; i < maxLength; i++) {
                String oldLine = i < oldLines.length ? oldLines[i] : "";
                String newLine = i < newLines.length ? newLines[i] : "";
                
                if (!oldLine.equals(newLine)) {
                    DiffChange change = new DiffChange();
                    change.lineNumber = i + 1;
                    change.oldLine = oldLine;
                    change.newLine = newLine;
                    change.type = oldLine.isEmpty() ? "ADD" : newLine.isEmpty() ? "DELETE" : "MODIFY";
                    changes.add(change);
                }
            }
            
            return new com.google.gson.Gson().toJson(changes);
        } catch (Exception e) {
            Log.e(TAG, "JSON diff generation failed", e);
            return "[]";
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
            case "context":
                return generateContextDiff(oldContent, newContent, oldFile, newFile);
            case "side-by-side":
                return generateSideBySideDiff(oldContent, newContent);
            case "html":
                return generateHtmlDiff(oldContent, newContent);
            case "json":
                return generateJsonDiff(oldContent, newContent);
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