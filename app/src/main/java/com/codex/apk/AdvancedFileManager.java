package com.codex.apk;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

/**
 * Advanced File Manager with diff generation, content validation, and smart updates.
 */
public class AdvancedFileManager {
    private static final String TAG = "AdvancedFileManager";
    private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
    private final Context context;
    private final File projectDir;

    // File change listener
    public interface FileChangeListener {
        void onFileCreated(File file);
        void onFileModified(File file);
        void onFileDeleted(File file);
        void onFileRenamed(File oldFile, File newFile);
        void onFileValidationFailed(File file, String reason);
        void onDiffGenerated(File file, String diff);
    }

    private FileChangeListener fileChangeListener;

    public void setFileChangeListener(FileChangeListener listener) {
        this.fileChangeListener = listener;
    }

    public AdvancedFileManager(Context context, File projectDir) {
        this.context = context;
        this.projectDir = projectDir;
    }

    /**
     * Smart file update with advanced features
     */
    public FileOperationResult smartUpdateFile(File file, String newContent, String updateType,
                                             boolean validateContent,
                                             String contentType, String errorHandling) throws IOException {
        FileOperationResult result = new FileOperationResult();

        try {
            // Read current content
            String currentContent = readFileContent(file);

            // Apply update based on type
            String finalContent = applyUpdateType(currentContent, newContent, updateType);

            // Validate content if requested
            if (validateContent) {
                ValidationResult validation = validateContent(finalContent, contentType);
                if (!validation.isValid()) {
                    if ("strict".equals(errorHandling)) {
                        throw new IllegalArgumentException("Content validation failed: " + validation.getReason());
                    }
                }
            }

            // Write the file
            writeFileContent(file, finalContent);

            // Generate diff
            String diff = generateDiff(currentContent, finalContent);
            result.setDiff(diff);

            result.setSuccess(true);
            result.setMessage("File updated successfully");

            if (fileChangeListener != null) {
                fileChangeListener.onFileModified(file);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Update failed: " + e.getMessage());
            Log.e(TAG, "Smart update failed", e);
        }

        return result;
    }

    /**
     * Apply different update types
     */
    private String applyUpdateType(String currentContent, String newContent, String updateType) {
        switch (updateType) {
            case "append":
                return currentContent + "\n" + newContent;
            case "prepend":
                return newContent + "\n" + currentContent;
            case "replace":
                // Replace entire content
                return newContent;
            case "smart":
                return applySmartUpdate(currentContent, newContent);
            case "patch":
                return applyPatch(currentContent, newContent);
            default:
                return newContent;
        }
    }

    /**
     * Smart update with intelligent merging
     */
    private String applySmartUpdate(String currentContent, String newContent) {
        // Simple smart merging - can be enhanced with more sophisticated algorithms
        String[] currentLines = currentContent.split("\n");
        String[] newLines = newContent.split("\n");

        // Find common patterns and merge intelligently
        StringBuilder result = new StringBuilder();

        // Add current content
        result.append(currentContent);

        // Add new content if it's not already present
        if (!currentContent.contains(newContent)) {
            result.append("\n").append(newContent);
        }

        return result.toString();
    }

    /**
     * Apply unified diff patch
     */
    private String applyPatch(String currentContent, String patchContent) {
        try {
            // Simple patch application - can be enhanced with proper diff library
            String[] lines = currentContent.split("\n");
            String[] patchLines = patchContent.split("\n");

            // Safety check for complex patches
            for (String patchLine : patchLines) {
                if (!patchLine.startsWith("+") && !patchLine.startsWith("-") && !patchLine.startsWith("@@") && !patchLine.startsWith("---") && !patchLine.startsWith("+++") && !patchLine.trim().isEmpty()) {
                    Log.w(TAG, "applyPatch: Found complex line in patch that cannot be handled: '" + patchLine + "'. Aborting patch.");
                    return currentContent; // Return original if patch is too complex for this simple parser
                }
            }

            List<String> result = new ArrayList<>(Arrays.asList(lines));

            for (String patchLine : patchLines) {
                if (patchLine.startsWith("+") && !patchLine.startsWith("+++")) {
                    // Add line
                    result.add(patchLine.substring(1));
                } else if (patchLine.startsWith("-") && !patchLine.startsWith("---")) {
                    // Remove line
                    String toRemove = patchLine.substring(1);
                    result.removeIf(line -> line.equals(toRemove));
                }
            }

            return String.join("\n", result);
        } catch (Exception e) {
            Log.e(TAG, "Patch application failed", e);
            return currentContent; // Return original if patch fails
        }
    }

    /**
     * Generate diff using enhanced diff generator
     */
    public String generateDiff(String oldContent, String newContent) {
        return generateDiff(oldContent, newContent, "unified", "original", "modified");
    }

    /**
     * Generate diff with specific format
     */
    public String generateDiff(String oldContent, String newContent, String format, String oldFile, String newFile) {
        try {
            return DiffGenerator.generateDiff(oldContent, newContent, format, oldFile, newFile);
        } catch (Exception e) {
            Log.e(TAG, "Enhanced diff generation failed", e);
            // Fallback to simple diff
            StringBuilder diff = new StringBuilder();
            diff.append("--- ").append(oldFile).append("\n");
            diff.append("+++ ").append(newFile).append("\n");

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
    }

    /**
     * Content validation
     */
    public ValidationResult validateContent(String content, String contentType) {
        ValidationResult result = new ValidationResult();

        if (content == null || content.trim().isEmpty()) {
            result.setValid(false);
            result.setReason("Content is empty");
            return result;
        }

        // File type specific validation
        if (contentType != null) {
            switch (contentType.toLowerCase()) {
                case "html":
                    if (!content.contains("<html") && !content.contains("<!DOCTYPE")) {
                        result.setValid(false);
                        result.setReason("Invalid HTML content");
                        return result;
                    }
                    break;
                case "css":
                    if (!content.contains("{") || !content.contains("}")) {
                        result.setValid(false);
                        result.setReason("Invalid CSS content");
                        return result;
                    }
                    break;
                case "javascript":
                case "js":
                    if (!content.contains("function") && !content.contains("var") && !content.contains("const")) {
                        result.setValid(false);
                        result.setReason("Invalid JavaScript content");
                        return result;
                    }
                    break;
            }
        }

        result.setValid(true);
        return result;
    }

    /**
     * Read file content
     */
    public String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Write file content
     */
    public void writeFileContent(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Result class for file operations
     */
    public static class FileOperationResult {
        private boolean success;
        private String message;
        private String diff;
        private String errorDetails;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getDiff() { return diff; }
        public void setDiff(String diff) { this.diff = diff; }

        public String getErrorDetails() { return errorDetails; }
        public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid;
        private String reason;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}