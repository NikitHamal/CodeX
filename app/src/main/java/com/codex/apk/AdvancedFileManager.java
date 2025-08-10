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

            // Backup before write if requested via updateType 'patch' or safety default
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            try {
                writeFileContent(backup, currentContent);
            } catch (Exception ignored) {}

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
            // Robust unified diff application (context-aware)
            return UnifiedDiff.applyUnifiedDiff(currentContent, patchContent);
        } catch (Exception e) {
            Log.e(TAG, "Patch application failed", e);
            return currentContent; // Return original if patch fails
        }
    }

    /**
     * Minimal unified diff applier supporting @@ hunks with context. Tolerates minor drift.
     */
    private static class UnifiedDiff {
        private static class Hunk {
            int startOld;
            int lenOld;
            int startNew;
            int lenNew;
            List<String> lines = new ArrayList<>();
        }

        public static String applyUnifiedDiff(String original, String patch) {
            List<String> src = new ArrayList<>(Arrays.asList(original.split("\n", -1)));
            List<Hunk> hunks = parseHunks(patch);
            // Apply hunks in order, adjust offsets as we go
            int offset = 0;
            for (Hunk h : hunks) {
                int applyPos = Math.max(0, Math.min(src.size(), h.startOld - 1 + offset));
                // Try exact context match first; if not, do a fuzzy search window
                int matchedIndex = findBestMatch(src, h, applyPos);
                if (matchedIndex < 0) continue; // skip hunk if no reasonable match
                // Remove old lines
                int removeCount = Math.min(h.lenOld, Math.max(0, src.size() - matchedIndex));
                for (int i = 0; i < removeCount; i++) {
                    src.remove(matchedIndex);
                }
                // Insert new lines
                List<String> toInsert = new ArrayList<>();
                for (String l : h.lines) if (l.length() > 0 && (l.charAt(0) == ' ' || l.charAt(0) == '+')) {
                    if (l.startsWith(" ") || l.startsWith("+")) {
                        String content = l.substring(1);
                        toInsert.add(content);
                    }
                }
                src.addAll(matchedIndex, toInsert);
                offset += toInsert.size() - removeCount;
            }
            return String.join("\n", src);
        }

        private static List<Hunk> parseHunks(String patch) {
            List<Hunk> hunks = new ArrayList<>();
            String[] lines = patch.split("\n");
            Hunk current = null;
            for (String line : lines) {
                if (line.startsWith("@@")) {
                    if (current != null) hunks.add(current);
                    current = new Hunk();
                    int[] nums = parseHunkHeader(line);
                    current.startOld = nums[0];
                    current.lenOld = nums[1];
                    current.startNew = nums[2];
                    current.lenNew = nums[3];
                } else if (current != null) {
                    if (line.startsWith("+") || line.startsWith("-") || line.startsWith(" ")) {
                        current.lines.add(line);
                    }
                }
            }
            if (current != null) hunks.add(current);
            return hunks;
        }

        private static int[] parseHunkHeader(String header) {
            // @@ -a,b +c,d @@
            try {
                String core = header.substring(2, header.indexOf("@@", 2)).trim();
                String[] parts = core.split(" ");
                String[] oldPart = parts[0].substring(1).split(",");
                String[] newPart = parts[1].substring(1).split(",");
                int a = parseIntSafe(oldPart[0], 1);
                int b = oldPart.length > 1 ? parseIntSafe(oldPart[1], 0) : 0;
                int c = parseIntSafe(newPart[0], 1);
                int d = newPart.length > 1 ? parseIntSafe(newPart[1], 0) : 0;
                return new int[]{a, b, c, d};
            } catch (Exception e) {
                return new int[]{1, 0, 1, 0};
            }
        }

        private static int parseIntSafe(String s, int def) {
            try { return Integer.parseInt(s); } catch (Exception e) { return def; }
        }

        private static int findBestMatch(List<String> src, Hunk h, int expectedIndex) {
            // Try exact expected index first
            if (contextMatches(src, expectedIndex, h)) return expectedIndex;
            // Fuzzy search in a small window around expected index
            int window = 50;
            for (int delta = 1; delta <= window; delta++) {
                int left = expectedIndex - delta;
                int right = expectedIndex + delta;
                if (left >= 0 && contextMatches(src, left, h)) return left;
                if (right <= src.size() && contextMatches(src, right, h)) return right;
            }
            return -1;
        }

        private static boolean contextMatches(List<String> src, int index, Hunk h) {
            // Compare context lines (' ' and '-' from old) against source
            int i = index;
            for (String l : h.lines) {
                if (l.startsWith(" ") || l.startsWith("-")) {
                    String want = l.length() > 0 ? l.substring(1) : "";
                    if (i >= src.size()) return false;
                    if (!src.get(i).equals(want)) return false;
                    i++;
                }
            }
            return true;
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