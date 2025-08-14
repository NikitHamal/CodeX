package com.codex.apk.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileOps {
    private FileOps() {}

    public static boolean deleteRecursively(File f) {
        if (f == null) return false;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        return f.delete();
    }

    public static String buildFileTree(File root, int maxDepth, int maxEntries) {
        StringBuilder sb = new StringBuilder();
        explore(root, 0, maxDepth, sb, new int[]{0}, maxEntries);
        return sb.toString();
    }

    private static void explore(File dir, int depth, int maxDepth, StringBuilder sb, int[] count, int maxEntries) {
        if (dir == null || !dir.exists() || count[0] >= maxEntries || depth > maxDepth) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File f : files) {
            if (count[0]++ >= maxEntries) return;
            for (int i = 0; i < depth; i++) sb.append("  ");
            sb.append(f.isDirectory() ? "[d] " : "[f] ").append(f.getName()).append("\n");
            if (f.isDirectory()) explore(f, depth + 1, maxDepth, sb, count, maxEntries);
        }
    }

    public static JsonArray searchInProject(File root, String query, int maxResults, boolean regex) {
        // Backward-compatible flavor: restrict to common web file extensions and return offsets/snippets
        List<String> exts = Arrays.asList("html", "htm", "css", "js", "json", "md");
        return searchInFilesOffsets(root, query, true, regex, exts, maxResults);
    }

    public static String autoFix(String path, String content, boolean aggressive) {
        if (path == null || content == null) return content;
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            String out = content;
            if (!out.toLowerCase().contains("<!doctype")) out = "<!DOCTYPE html>\n" + out;
            if (out.toLowerCase().contains("<img ") && !out.toLowerCase().contains(" alt=")) {
                out = out.replaceAll("<img ", "<img alt=\"\" ");
            }
            return out;
        }
        if (lower.endsWith(".css")) {
            int open = 0; for (int i=0;i<content.length();i++){ char c=content.charAt(i); if (c=='{') open++; else if (c=='}') open--; }
            StringBuilder out = new StringBuilder(content);
            while (open>0) { out.append("}\n"); open--; }
            return out.toString();
        }
        if (lower.endsWith(".js")) {
            int par=0, brc=0, brk=0; for (int i=0;i<content.length();i++){ char c=content.charAt(i); if (c=='(') par++; else if(c==')') par--; if (c=='{') brc++; else if(c=='}') brc--; if (c=='[') brk++; else if(c==']') brk--; }
            StringBuilder out = new StringBuilder(content);
            while (par>0){ out.append(')'); par--; }
            while (brc>0){ out.append('}'); brc--; }
            while (brk>0){ out.append(']'); brk--; }
            return out.toString();
        }
        return content;
    }

    public static String readFileSafe(File f) {
        try {
            if (f != null && f.exists()) {
                return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
        return "";
    }

    public static String applySearchReplace(String input, String pattern, String replacement) {
        if (input == null) return "";
        if (pattern == null || pattern.isEmpty()) return input;
        String repl = replacement != null ? replacement : "";
        try {
            return input.replaceAll(pattern, repl);
        } catch (Exception e) {
            return input.replace(pattern, repl);
        }
    }

    public static String applyModifyLines(String content, int startLine, int deleteCount, List<String> insertLines) {
        if (content == null) return "";
        String[] lines = content.split("\n", -1);
        List<String> out = new java.util.ArrayList<>();
        for (String l : lines) out.add(l);
        int idx = Math.max(0, Math.min(out.size(), startLine > 0 ? startLine - 1 : 0));
        int toDelete = Math.max(0, Math.min(deleteCount, out.size() - idx));
        for (int i = 0; i < toDelete; i++) {
            out.remove(idx);
        }
        if (insertLines != null && !insertLines.isEmpty()) {
            out.addAll(idx, insertLines);
        }
        return String.join("\n", out);
    }

    // ===== Consolidated search/file listing helpers (migrated from FileSearchHelper) =====

    // Line-number oriented search result flavor
    public static class LineSearchResult {
        private final File file;
        private final String fileName;
        private final int lineNumber;
        private final String lineContent;
        private final String matchedText;

        public LineSearchResult(File file, String fileName, int lineNumber, String lineContent, String matchedText) {
            this.file = file;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchedText = matchedText;
        }

        public File getFile() { return file; }
        public String getFileName() { return fileName; }
        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
        public String getMatchedText() { return matchedText; }
    }

    // Public: search by file name (simple contains match respecting case sensitivity)
    public static List<File> searchFilesByName(File projectDir, String pattern, boolean caseSensitive) {
        List<File> results = new ArrayList<>();
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory() || pattern == null) return results;
        String probe = caseSensitive ? pattern : pattern.toLowerCase();
        searchFilesByNameRecursive(projectDir, probe, caseSensitive, results);
        return results;
    }

    private static void searchFilesByNameRecursive(File dir, String pattern, boolean caseSensitive, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (!name.startsWith(".") && !name.equals("node_modules") && !name.equals("build") && !name.equals("dist")) {
                    searchFilesByNameRecursive(file, pattern, caseSensitive, results);
                }
            } else {
                String name = file.getName();
                String target = caseSensitive ? name : name.toLowerCase();
                if (target.contains(pattern)) results.add(file);
            }
        }
    }

    // Public: search in files and return line-number oriented results
    public static List<LineSearchResult> searchInFiles(File projectDir, String searchText, boolean caseSensitive,
                                                       boolean useRegex, List<String> fileExtensions, int maxResults) {
        List<LineSearchResult> results = new ArrayList<>();
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory() || searchText == null || searchText.trim().isEmpty()) {
            return results;
        }
        Pattern searchPattern = null;
        if (useRegex) {
            try { searchPattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE); }
            catch (Exception ignored) { useRegex = false; }
        }
        searchInFilesRecursive(projectDir, searchText, searchPattern, caseSensitive, useRegex, fileExtensions, results, maxResults);
        return results;
    }

    private static void searchInFilesRecursive(File dir, String searchText, Pattern searchPattern,
                                               boolean caseSensitive, boolean useRegex, List<String> fileExtensions,
                                               List<LineSearchResult> results, int maxResults) {
        if (results.size() >= maxResults) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (results.size() >= maxResults) break;
            if (file.isDirectory()) {
                String dirName = file.getName();
                if (!dirName.startsWith(".") && !dirName.equals("node_modules") && !dirName.equals("build") && !dirName.equals("dist")) {
                    searchInFilesRecursive(file, searchText, searchPattern, caseSensitive, useRegex, fileExtensions, results, maxResults);
                }
            } else {
                if (fileExtensions != null && !fileExtensions.isEmpty()) {
                    String fileNameLower = file.getName().toLowerCase();
                    boolean ok = false;
                    for (String ext : fileExtensions) {
                        if (fileNameLower.endsWith("." + ext.toLowerCase())) { ok = true; break; }
                    }
                    if (!ok) continue;
                }
                searchInFile(file, searchText, searchPattern, caseSensitive, useRegex, results, maxResults);
            }
        }
    }

    private static void searchInFile(File file, String searchText, Pattern searchPattern,
                                     boolean caseSensitive, boolean useRegex, List<LineSearchResult> results, int maxResults) {
        if (results.size() >= maxResults) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line; int lineNumber = 1;
            while ((line = reader.readLine()) != null && results.size() < maxResults) {
                String searchLine = caseSensitive ? line : line.toLowerCase();
                String target = caseSensitive ? searchText : searchText.toLowerCase();
                if (useRegex && searchPattern != null) {
                    if (searchPattern.matcher(line).find()) {
                        results.add(new LineSearchResult(file, file.getName(), lineNumber, line.trim(), searchText));
                    }
                } else {
                    if (searchLine.contains(target)) {
                        results.add(new LineSearchResult(file, file.getName(), lineNumber, line.trim(), searchText));
                    }
                }
                lineNumber++;
            }
        } catch (IOException ignored) {}
    }

    // Public: search in files and return offset/snippet oriented results
    public static JsonArray searchInFilesOffsets(File projectDir, String searchText, boolean caseSensitive,
                                                 boolean useRegex, List<String> fileExtensions, int maxResults) {
        JsonArray out = new JsonArray();
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory() || searchText == null || searchText.trim().isEmpty()) return out;
        Pattern pattern = null;
        if (useRegex) {
            try { pattern = Pattern.compile(searchText, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.MULTILINE); }
            catch (Exception ignored) { useRegex = false; }
        }
        Deque<File> dq = new ArrayDeque<>(); dq.add(projectDir);
        while (!dq.isEmpty() && out.size() < maxResults) {
            File cur = dq.pollFirst();
            File[] files = cur != null ? cur.listFiles() : null; if (files == null) continue;
            for (File f : files) {
                if (out.size() >= maxResults) break;
                if (f.isDirectory()) {
                    String dn = f.getName();
                    if (!dn.startsWith(".") && !dn.equals("node_modules") && !dn.equals("build") && !dn.equals("dist")) dq.addLast(f);
                    continue;
                }
                if (fileExtensions != null && !fileExtensions.isEmpty()) {
                    String lower = f.getName().toLowerCase(); boolean ok = false;
                    for (String ext : fileExtensions) { if (lower.endsWith("." + ext.toLowerCase())) { ok = true; break; } }
                    if (!ok) continue;
                }
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    if (useRegex && pattern != null) {
                        Matcher m = pattern.matcher(content); int hits = 0;
                        while (m.find() && out.size() < maxResults) {
                            JsonObject o = new JsonObject();
                            o.addProperty("path", projectDir.toPath().relativize(f.toPath()).toString());
                            o.addProperty("start", m.start());
                            o.addProperty("end", m.end());
                            int s = Math.max(0, m.start() - 80); int e = Math.min(content.length(), m.end() + 80);
                            o.addProperty("snippet", content.substring(s, e));
                            out.add(o);
                            if (++hits > 10) break;
                        }
                    } else {
                        String hay = caseSensitive ? content : content.toLowerCase();
                        String needle = caseSensitive ? searchText : searchText.toLowerCase();
                        int from = 0; int hits = 0;
                        while (from <= hay.length() && out.size() < maxResults) {
                            int idx = hay.indexOf(needle, from);
                            if (idx < 0) break;
                            JsonObject o = new JsonObject();
                            o.addProperty("path", projectDir.toPath().relativize(f.toPath()).toString());
                            o.addProperty("start", idx);
                            o.addProperty("end", idx + needle.length());
                            int s = Math.max(0, idx - 80); int e = Math.min(content.length(), idx + needle.length() + 80);
                            o.addProperty("snippet", content.substring(s, e));
                            out.add(o);
                            from = idx + Math.max(1, needle.length());
                            if (++hits > 10) break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    // Recent files helper
    public static List<File> getRecentFiles(File projectDir, int maxFiles) {
        List<File> files = new ArrayList<>();
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory()) return files;
        collectAllFiles(projectDir, files);
        files.sort(Comparator.comparingLong(File::lastModified).reversed());
        if (files.size() > maxFiles) return new ArrayList<>(files.subList(0, Math.max(0, maxFiles)));
        return files;
    }

    private static void collectAllFiles(File dir, List<File> out) {
        File[] arr = dir.listFiles(); if (arr == null) return;
        for (File f : arr) {
            if (f.isDirectory()) {
                String dn = f.getName();
                if (!dn.startsWith(".") && !dn.equals("node_modules") && !dn.equals("build") && !dn.equals("dist")) collectAllFiles(f, out);
            } else {
                out.add(f);
            }
        }
    }

    // Convenience helpers using projectDir and relative paths
    public static void createFile(File projectDir, String relativePath, String content) throws java.io.IOException {
        File file = new File(projectDir, relativePath);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Files.write(file.toPath(), (content != null ? content : "").getBytes(StandardCharsets.UTF_8));
    }

    public static void updateFile(File projectDir, String relativePath, String content) throws java.io.IOException {
        File file = new File(projectDir, relativePath);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        Files.write(file.toPath(), (content != null ? content : "").getBytes(StandardCharsets.UTF_8));
    }

    public static boolean renameFile(File projectDir, String oldPath, String newPath) {
        File oldFile = new File(projectDir, oldPath);
        File newFile = new File(projectDir, newPath);
        File parent = newFile.getParentFile();
        if (parent != null) parent.mkdirs();
        return oldFile.renameTo(newFile);
    }

    public static String readFile(File projectDir, String relativePath) throws java.io.IOException {
        File file = new File(projectDir, relativePath);
        if (!file.exists()) return null;
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public static String autoFix(File projectDir, String relativePath, boolean aggressive) throws java.io.IOException {
        String content = readFile(projectDir, relativePath);
        if (content == null) return null;
        return autoFix(relativePath, content, aggressive);
    }
}
