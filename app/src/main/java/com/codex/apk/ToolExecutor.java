package com.codex.apk;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.codex.apk.util.FileOps;

import java.io.File;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Model-agnostic tool executor used by the UI layer to run tool_calls
 * when models do not natively support executing tools.
 */
public class ToolExecutor {
    private static final OkHttpClient httpClient = new OkHttpClient();

    public static JsonObject execute(File projectDir, String name, JsonObject args) {
        JsonObject result = new JsonObject();
        try {
            switch (name) {
                case "listProjectTree": {
                    String path = args.has("path") ? args.get("path").getAsString() : ".";
                    int depth = args.has("depth") ? Math.max(0, Math.min(5, args.get("depth").getAsInt())) : 2;
                    int maxEntries = args.has("maxEntries") ? Math.max(10, Math.min(2000, args.get("maxEntries").getAsInt())) : 500;
                    String tree = FileOps.buildFileTree(new File(projectDir, path), depth, maxEntries);
                    result.addProperty("ok", true);
                    result.addProperty("tree", tree);
                    break;
                }
                case "searchInProject": {
                    String query = args.get("query").getAsString();
                    int maxResults = args.has("maxResults") ? Math.max(1, Math.min(2000, args.get("maxResults").getAsInt())) : 100;
                    boolean regex = args.has("regex") && args.get("regex").getAsBoolean();
                    JsonArray matches = FileOps.searchInProject(projectDir, query, maxResults, regex);
                    result.addProperty("ok", true);
                    result.add("matches", matches);
                    break;
                }
                case "createFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    FileOps.createFile(projectDir, path, content);
                    result.addProperty("ok", true);
                    result.addProperty("message", "File created: " + path);
                    break;
                }
                case "updateFile": {
                    String path = args.get("path").getAsString();
                    String content = args.get("content").getAsString();
                    FileOps.updateFile(projectDir, path, content);
                    result.addProperty("ok", true);
                    result.addProperty("message", "File updated: " + path);
                    break;
                }
                case "deleteFile": {
                    String path = args.get("path").getAsString();
                    boolean deleted = FileOps.deleteRecursively(new File(projectDir, path));
                    result.addProperty("ok", deleted);
                    result.addProperty("message", "Deleted: " + path);
                    break;
                }
                case "renameFile": {
                    String oldPath = args.get("oldPath").getAsString();
                    String newPath = args.get("newPath").getAsString();
                    boolean ok = FileOps.renameFile(projectDir, oldPath, newPath);
                    result.addProperty("ok", ok);
                    result.addProperty("message", "Renamed to: " + newPath);
                    break;
                }
                case "fixLint": {
                    String path = args.get("path").getAsString();
                    boolean aggressive = args.has("aggressive") && args.get("aggressive").getAsBoolean();
                    String fixed = FileOps.autoFix(projectDir, path, aggressive);
                    if (fixed == null) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found");
                        break;
                    }
                    FileOps.updateFile(projectDir, path, fixed);
                    result.addProperty("ok", true);
                    result.addProperty("message", "Applied basic lint fixes");
                    break;
                }
                case "readFile": {
                    String path = args.get("path").getAsString();
                    String content = FileOps.readFile(projectDir, path);
                    if (content == null) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "File not found: " + path);
                    } else {
                        result.addProperty("ok", true);
                        int maxLength = 20000;
                        if (content.length() > maxLength) {
                            content = content.substring(0, maxLength);
                            result.addProperty("message", "File read (truncated): " + path);
                        } else {
                            result.addProperty("message", "File read: " + path);
                        }
                        result.addProperty("content", content);
                    }
                    break;
                }
                case "listFiles": {
                    String path = args.get("path").getAsString();
                    File dir = new File(projectDir, path);
                    if (!dir.exists() || !dir.isDirectory()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "Directory not found: " + path);
                    } else {
                        JsonArray files = new JsonArray();
                        File[] fileList = dir.listFiles();
                        if (fileList != null) {
                            for (File f : fileList) {
                                JsonObject fileInfo = new JsonObject();
                                fileInfo.addProperty("name", f.getName());
                                fileInfo.addProperty("type", f.isDirectory() ? "directory" : "file");
                                fileInfo.addProperty("size", f.length());
                                files.add(fileInfo);
                            }
                        }
                        result.addProperty("ok", true);
                        result.add("files", files);
                        result.addProperty("message", "Directory listed: " + path);
                    }
                    break;
                }
                case "readUrlContent": {
                    String url = args.get("url").getAsString();
                    Request request = new Request.Builder().url(url).get().addHeader("Accept", "*/*").build();
                    try (Response resp = httpClient.newCall(request).execute()) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            String content = resp.body().string();
                            String type = resp.header("Content-Type", "");
                            int max = 200_000;
                            if (content.length() > max) content = content.substring(0, max);
                            result.addProperty("ok", true);
                            result.addProperty("content", content);
                            result.addProperty("contentType", type);
                            result.addProperty("status", resp.code());
                        } else {
                            result.addProperty("ok", false);
                            result.addProperty("error", "HTTP " + (resp != null ? resp.code() : 0));
                        }
                    }
                    break;
                }
                case "grepSearch": {
                    String query = args.get("query").getAsString();
                    String path = args.has("path") ? args.get("path").getAsString() : ".";
                    boolean isRegex = args.has("isRegex") && args.get("isRegex").getAsBoolean();
                    boolean caseInsensitive = args.has("caseInsensitive") && args.get("caseInsensitive").getAsBoolean();
                    boolean caseSensitive = !caseInsensitive;
                    File root = new File(projectDir, path);
                    // Use existing offset/snippet search; no extension filter, cap results
                    JsonArray results = FileOps.searchInFilesOffsets(root, query, caseSensitive, isRegex, new java.util.ArrayList<>(), 500);
                    result.addProperty("ok", true);
                    result.add("results", results);
                    break;
                }
                default: {
                    result.addProperty("ok", false);
                    result.addProperty("error", "Unknown tool: " + name);
                }
            }
        } catch (Exception e) {
            result.addProperty("ok", false);
            result.addProperty("error", e.getMessage());
        }
        return result;
    }

    /** Build the tool_result continuation payload matching our prompt contract. */
    public static String buildToolResultContinuation(JsonArray results) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "tool_result");
        payload.add("results", results);
        return payload.toString();
    }
}
