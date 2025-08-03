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
 * Advanced File Manager with versioning, diff generation, content validation, and smart updates.
 * Merges updateFile and searchAndReplace functionality with advanced features.
 */
public class AdvancedFileManager {
    private static final String TAG = "AdvancedFileManager";
    private final Pattern autoInvalidFileNameChars = Pattern.compile("[\\\\/:*?\"<>|]");
    private final Context context;
    private final File projectDir;
    private final File versionsDir;
    private final File backupsDir;
    
    // File change listener
    public interface FileChangeListener {
        void onFileCreated(File file);
        void onFileModified(File file);
        void onFileDeleted(File file);
        void onFileRenamed(File oldFile, File newFile);
        void onFileBackedUp(File originalFile, File backupFile);
        void onFileVersioned(File file, String versionId);
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
        this.versionsDir = new File(projectDir, ".versions");
        this.backupsDir = new File(projectDir, ".backups");
        
        // Create version and backup directories
        versionsDir.mkdirs();
        backupsDir.mkdirs();
    }

    /**
     * Smart file update with advanced features
     */
    public FileOperationResult smartUpdateFile(File file, String newContent, String updateType, 
                                             boolean createBackup, boolean validateContent, 
                                             String contentType, String errorHandling) throws IOException {
        FileOperationResult result = new FileOperationResult();
        
        try {
            // Read current content
            String currentContent = readFileContent(file);
            
            // Create backup if requested
            File backupFile = null;
            if (createBackup && file.exists()) {
                backupFile = createBackup(file);
                result.setBackupFile(backupFile);
            }
            
            // Apply update based on type
            String finalContent = applyUpdateType(currentContent, newContent, updateType);
            
            // Validate content if requested
            if (validateContent) {
                ValidationResult validation = validateContent(finalContent, contentType);
                if (!validation.isValid()) {
                    if ("strict".equals(errorHandling)) {
                        throw new IllegalArgumentException("Content validation failed: " + validation.getReason());
                    } else if ("auto-revert".equals(errorHandling) && backupFile != null) {
                        // Revert to backup
                        writeFileContent(file, readFileContent(backupFile));
                        result.setReverted(true);
                        result.setMessage("Content validation failed, reverted to backup");
                        return result;
                    }
                }
            }
            
            // Write the file
            writeFileContent(file, finalContent);
            
            // Generate diff
            String diff = generateDiff(currentContent, finalContent);
            result.setDiff(diff);
            
            // Create version
            String versionId = createVersion(file, currentContent, finalContent, diff);
            result.setVersionId(versionId);
            
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
     * Create version with metadata
     */
    public String createVersion(File file, String oldContent, String newContent, String diff) throws IOException {
        String versionId = generateVersionId();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        
        File versionFile = new File(versionsDir, file.getName() + "." + versionId + ".version");
        
        Map<String, Object> versionData = new HashMap<>();
        versionData.put("versionId", versionId);
        versionData.put("timestamp", timestamp);
        versionData.put("filePath", file.getPath());
        versionData.put("oldContent", oldContent);
        versionData.put("newContent", newContent);
        versionData.put("diff", diff);
        versionData.put("fileSize", file.length());
        versionData.put("contentHash", generateContentHash(newContent));
        
        // Write version data
        try (FileOutputStream fos = new FileOutputStream(versionFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
            
            String jsonData = new com.google.gson.Gson().toJson(versionData);
            writer.write(jsonData);
        }
        
        if (fileChangeListener != null) {
            fileChangeListener.onFileVersioned(file, versionId);
        }
        
        return versionId;
    }

    /**
     * Create backup file
     */
    public File createBackup(File originalFile) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String backupName = originalFile.getName() + ".backup." + timestamp;
        File backupFile = new File(backupsDir, backupName);
        
        // Copy file content
        try (FileInputStream fis = new FileInputStream(originalFile);
             FileOutputStream fos = new FileOutputStream(backupFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        if (fileChangeListener != null) {
            fileChangeListener.onFileBackedUp(originalFile, backupFile);
        }
        
        return backupFile;
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
     * Generate version ID
     */
    private String generateVersionId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate content hash
     */
    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "hash_error";
        }
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
        private String versionId;
        private String diff;
        private File backupFile;
        private boolean reverted;
        private String errorDetails;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getVersionId() { return versionId; }
        public void setVersionId(String versionId) { this.versionId = versionId; }
        
        public String getDiff() { return diff; }
        public void setDiff(String diff) { this.diff = diff; }
        
        public File getBackupFile() { return backupFile; }
        public void setBackupFile(File backupFile) { this.backupFile = backupFile; }
        
        public boolean isReverted() { return reverted; }
        public void setReverted(boolean reverted) { this.reverted = reverted; }
        
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