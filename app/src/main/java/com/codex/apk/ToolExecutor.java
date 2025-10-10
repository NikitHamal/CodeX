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
                    String relPath = args.has("path") ? args.get("path").getAsString() : ".";
                    boolean isRegex = args.has("isRegex") && args.get("isRegex").getAsBoolean();
                    boolean caseInsensitive = args.has("caseInsensitive") && args.get("caseInsensitive").getAsBoolean();

                    java.io.File start = new java.io.File(projectDir, relPath);
                    if (!start.exists()) {
                        result.addProperty("ok", false);
                        result.addProperty("error", "Path not found: " + relPath);
                        break;
                    }

                    int flags = caseInsensitive ? java.util.regex.Pattern.CASE_INSENSITIVE : 0;
                    java.util.regex.Pattern pattern = isRegex
                            ? java.util.regex.Pattern.compile(query, flags)
                            : java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(query), flags);

                    JsonArray matches = new JsonArray();
                    final int maxMatches = 2000;
                    grepWalk(start, projectDir, pattern, matches, new int[]{0}, maxMatches);
                    result.addProperty("ok", true);
                    result.add("matches", matches);
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

    private static void grepWalk(java.io.File file, java.io.File projectRoot, java.util.regex.Pattern pattern,
                          com.google.gson.JsonArray outMatches, int[] count, int maxMatches) {
        if (count[0] >= maxMatches || file == null || !file.exists()) return;
        if (file.isDirectory()) {
            String name = file.getName();
            if (shouldSkipDir(name)) return;
            java.io.File[] children = file.listFiles();
            if (children == null) return;
            for (java.io.File c : children) {
                if (count[0] >= maxMatches) break;
                grepWalk(c, projectRoot, pattern, outMatches, count, maxMatches);
            }
            return;
        }

        // Skip large/binary files
        long maxSize = 2_000_000; // 2MB
        if (file.length() > maxSize || looksBinary(file)) return;

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (pattern.matcher(line).find()) {
                    com.google.gson.JsonObject m = new com.google.gson.JsonObject();
                    m.addProperty("file", relPath(projectRoot, file));
                    m.addProperty("line", lineNo);
                    // Limit line length in output
                    String text = line;
                    int maxLen = 500;
                    if (text.length() > maxLen) text = text.substring(0, maxLen);
                    m.addProperty("text", text);
                    outMatches.add(m);
                    count[0]++;
                    if (count[0] >= maxMatches) break;
                }
            }
        } catch (Exception ignored) { }
    }

    private static boolean shouldSkipDir(String name) {
        if (name == null) return true;
        String n = name.toLowerCase();
        return n.equals(".git") || n.equals(".gradle") || n.equals("build") || n.equals("dist") ||
               n.equals("node_modules") || n.equals(".idea") || n.equals("out") || n.equals(".next") ||
               n.equals(".nuxt") || n.equals("target");
    }

    private static boolean looksBinary(java.io.File f) {
        // Heuristic: read first 4096 bytes; if there are NULs or many non-text chars, treat as binary
        int sample = 4096;
        byte[] buf = new byte[sample];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(f)) {
            int read = fis.read(buf);
            if (read <= 0) return false;
            int nonText = 0;
            for (int i = 0; i < read; i++) {
                int b = buf[i] & 0xFF;
                if (b == 0) return true; // NUL byte
                // Allow common text control chars: tab, CR, LF, FF
                if (b < 0x09 || (b > 0x0D && b < 0x20)) nonText++;
            }
            return nonText > read * 0.3; // >30% suspicious
        } catch (Exception e) {
            return false;
        }
    }

    private static String relPath(java.io.File root, java.io.File file) {
        try {
            String rp = root.getCanonicalPath();
            String fp = file.getCanonicalPath();
            if (fp.startsWith(rp)) {
                String r = fp.substring(rp.length());
                if (r.startsWith(java.io.File.separator)) r = r.substring(1);
                return r.replace('\\', '/');
            }
        } catch (Exception ignored) {}
        return file.getPath().replace('\\', '/');
    }

    /** Build the tool_result continuation payload matching our prompt contract. */
    public static String buildToolResultContinuation(JsonArray results) {
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "tool_result");
        payload.add("results", results);
        return payload.toString();
    }
}
