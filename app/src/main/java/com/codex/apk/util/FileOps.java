package com.codex.apk.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
        JsonArray out = new JsonArray();
        Pattern pattern = null;
        if (regex) {
            try { pattern = Pattern.compile(query, Pattern.MULTILINE); } catch (Exception ignored) {}
        }
        Deque<File> dq = new ArrayDeque<>();
        dq.add(root);
        while (!dq.isEmpty() && out.size() < maxResults) {
            File cur = dq.pollFirst();
            File[] files = cur != null ? cur.listFiles() : null;
            if (files == null) continue;
            for (File f : files) {
                if (f.isDirectory()) { dq.addLast(f); continue; }
                String name = f.getName().toLowerCase();
                if (!(name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".css") || name.endsWith(".js") || name.endsWith(".json") || name.endsWith(".md")))
                    continue;
                try {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    if (regex) {
                        if (pattern == null) continue;
                        Matcher m = pattern.matcher(content);
                        int hits = 0;
                        while (m.find() && out.size() < maxResults) {
                            JsonObject mobj = new JsonObject();
                            mobj.addProperty("path", root.toPath().relativize(f.toPath()).toString());
                            mobj.addProperty("start", m.start());
                            mobj.addProperty("end", m.end());
                            mobj.addProperty("snippet", content.substring(Math.max(0, m.start()-80), Math.min(content.length(), m.end()+80)));
                            out.add(mobj);
                            if (++hits > 10) break;
                        }
                    } else {
                        int idx = content.indexOf(query);
                        if (idx >= 0) {
                            JsonObject mobj = new JsonObject();
                            mobj.addProperty("path", root.toPath().relativize(f.toPath()).toString());
                            mobj.addProperty("start", idx);
                            mobj.addProperty("end", idx + query.length());
                            mobj.addProperty("snippet", content.substring(Math.max(0, idx-80), Math.min(content.length(), idx+80)));
                            out.add(mobj);
                        }
                    }
                } catch (Exception ignored) {}
                if (out.size() >= maxResults) break;
            }
        }
        return out;
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
