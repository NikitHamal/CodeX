package com.codex.apk;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Advanced Version Manager for file versioning, rollbacks, and history tracking.
 * Provides comprehensive version control capabilities for the AI code editor.
 */
public class VersionManager {
    private static final String TAG = "VersionManager";
    private final File versionsDir;
    private final File backupsDir;
    private final Context context;
    private final Gson gson;

    public VersionManager(Context context, File projectDir) {
        this.context = context;
        this.versionsDir = new File(projectDir, ".versions");
        this.backupsDir = new File(projectDir, ".backups");
        this.gson = new Gson();
        
        // Create directories if they don't exist
        versionsDir.mkdirs();
        backupsDir.mkdirs();
    }

    /**
     * Create a new version for a file
     */
    public VersionInfo createVersion(File file, String oldContent, String newContent, String diff, String operationType) throws IOException {
        String versionId = generateVersionId();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        
        VersionInfo versionInfo = new VersionInfo();
        versionInfo.versionId = versionId;
        versionInfo.timestamp = timestamp;
        versionInfo.filePath = file.getPath();
        versionInfo.fileName = file.getName();
        versionInfo.oldContent = oldContent;
        versionInfo.newContent = newContent;
        versionInfo.diff = diff;
        versionInfo.operationType = operationType;
        versionInfo.fileSize = file.length();
        versionInfo.contentHash = generateContentHash(newContent);
        versionInfo.metadata = new HashMap<>();
        
        // Save version data
        saveVersionData(versionInfo);
        
        Log.i(TAG, "Created version " + versionId + " for " + file.getName());
        return versionInfo;
    }

    /**
     * Get version history for a file
     */
    public List<VersionInfo> getVersionHistory(File file) {
        List<VersionInfo> versions = new ArrayList<>();
        
        try {
            File[] versionFiles = versionsDir.listFiles((dir, name) -> 
                name.startsWith(file.getName() + ".") && name.endsWith(".version"));
            
            if (versionFiles != null) {
                for (File versionFile : versionFiles) {
                    VersionInfo version = loadVersionData(versionFile);
                    if (version != null) {
                        versions.add(version);
                    }
                }
                
                // Sort by timestamp (newest first)
                versions.sort((v1, v2) -> v2.timestamp.compareTo(v1.timestamp));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading version history", e);
        }
        
        return versions;
    }

    /**
     * Rollback to a specific version
     */
    public boolean rollbackToVersion(File file, String versionId) {
        try {
            VersionInfo targetVersion = getVersionById(file, versionId);
            if (targetVersion == null) {
                Log.e(TAG, "Version " + versionId + " not found for " + file.getName());
                return false;
            }
            
            // Create backup of current state
            String currentContent = readFileContent(file);
            VersionInfo currentVersion = createVersion(file, currentContent, targetVersion.newContent, 
                DiffGenerator.generateDiff(currentContent, targetVersion.newContent, "unified", "current", targetVersion.versionId), "rollback");
            
            // Restore the target version
            writeFileContent(file, targetVersion.newContent);
            
            Log.i(TAG, "Rolled back " + file.getName() + " to version " + versionId);
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Rollback failed", e);
            return false;
        }
    }

    /**
     * Compare two versions
     */
    public String compareVersions(File file, String versionId1, String versionId2) {
        try {
            VersionInfo version1 = getVersionById(file, versionId1);
            VersionInfo version2 = getVersionById(file, versionId2);
            
            if (version1 == null || version2 == null) {
                return "One or both versions not found";
            }
            
            return DiffGenerator.generateDiff(version1.newContent, version2.newContent, "unified", 
                version1.versionId, version2.versionId);
            
        } catch (Exception e) {
            Log.e(TAG, "Version comparison failed", e);
            return "Version comparison failed: " + e.getMessage();
        }
    }

    /**
     * Get version statistics
     */
    public VersionStats getVersionStats(File file) {
        List<VersionInfo> versions = getVersionHistory(file);
        VersionStats stats = new VersionStats();
        
        stats.totalVersions = versions.size();
        stats.firstVersion = versions.isEmpty() ? null : versions.get(versions.size() - 1);
        stats.latestVersion = versions.isEmpty() ? null : versions.get(0);
        
        if (!versions.isEmpty()) {
            stats.totalChanges = versions.stream()
                .mapToInt(v -> v.diff != null ? v.diff.split("\n").length : 0)
                .sum();
            
            // Calculate average file size
            stats.averageFileSize = versions.stream()
                .mapToLong(v -> v.fileSize)
                .average()
                .orElse(0.0);
        }
        
        return stats;
    }

    /**
     * Clean up old versions
     */
    public int cleanupOldVersions(File file, int keepCount) {
        List<VersionInfo> versions = getVersionHistory(file);
        
        if (versions.size() <= keepCount) {
            return 0;
        }
        
        int deletedCount = 0;
        List<VersionInfo> toDelete = versions.subList(keepCount, versions.size());
        
        for (VersionInfo version : toDelete) {
            File versionFile = new File(versionsDir, version.fileName + "." + version.versionId + ".version");
            if (versionFile.delete()) {
                deletedCount++;
            }
        }
        
        Log.i(TAG, "Cleaned up " + deletedCount + " old versions for " + file.getName());
        return deletedCount;
    }

    /**
     * Export version history
     */
    public String exportVersionHistory(File file) {
        try {
            List<VersionInfo> versions = getVersionHistory(file);
            JsonObject export = new JsonObject();
            export.addProperty("fileName", file.getName());
            export.addProperty("exportDate", new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
            export.addProperty("totalVersions", versions.size());
            
            JsonArray versionsArray = new JsonArray();
            for (VersionInfo version : versions) {
                JsonObject versionObj = new JsonObject();
                versionObj.addProperty("versionId", version.versionId);
                versionObj.addProperty("timestamp", version.timestamp);
                versionObj.addProperty("operationType", version.operationType);
                versionObj.addProperty("fileSize", version.fileSize);
                versionObj.addProperty("contentHash", version.contentHash);
                versionObj.addProperty("diff", version.diff);
                versionsArray.add(versionObj);
            }
            export.add("versions", versionsArray);
            
            return gson.toJson(export);
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return "Export failed: " + e.getMessage();
        }
    }

    /**
     * Get version by ID
     */
    private VersionInfo getVersionById(File file, String versionId) {
        List<VersionInfo> versions = getVersionHistory(file);
        return versions.stream()
            .filter(v -> v.versionId.equals(versionId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Save version data to file
     */
    private void saveVersionData(VersionInfo versionInfo) throws IOException {
        File versionFile = new File(versionsDir, 
            versionInfo.fileName + "." + versionInfo.versionId + ".version");
        
        try (FileOutputStream fos = new FileOutputStream(versionFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {
            
            String jsonData = gson.toJson(versionInfo);
            writer.write(jsonData);
        }
    }

    /**
     * Load version data from file
     */
    private VersionInfo loadVersionData(File versionFile) {
        try (FileInputStream fis = new FileInputStream(versionFile);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             InputStreamReader reader = new InputStreamReader(gzis, StandardCharsets.UTF_8)) {
            
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, bytesRead);
            }
            
            return gson.fromJson(content.toString(), VersionInfo.class);
        } catch (Exception e) {
            Log.e(TAG, "Error loading version data from " + versionFile.getName(), e);
            return null;
        }
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
    private String readFileContent(File file) throws IOException {
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
    private void writeFileContent(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Version information class
     */
    public static class VersionInfo {
        public String versionId;
        public String timestamp;
        public String filePath;
        public String fileName;
        public String oldContent;
        public String newContent;
        public String diff;
        public String operationType;
        public long fileSize;
        public String contentHash;
        public Map<String, Object> metadata;
    }

    /**
     * Version statistics class
     */
    public static class VersionStats {
        public int totalVersions;
        public int totalChanges;
        public double averageFileSize;
        public VersionInfo firstVersion;
        public VersionInfo latestVersion;
    }
}