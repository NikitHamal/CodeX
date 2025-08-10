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
            String[] a = oldContent.split("\n", -1);
            String[] b = newContent.split("\n", -1);
            // Myers diff
            List<Edit> edits = myersDiff(a, b);
            StringBuilder out = new StringBuilder();
            out.append("--- ").append(oldFile).append("\n");
            out.append("+++ ").append(newFile).append("\n");
            for (Hunk h : buildHunks(a, b, edits, 3)) {
                out.append(String.format("@@ -%d,%d +%d,%d @@\n", h.aStart + 1, h.aLen, h.bStart + 1, h.bLen));
                for (String s : h.lines) out.append(s).append("\n");
            }
            return out.toString();
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

    // --- Minimal Myers diff implementation for line sequences ---
    private static class Edit {
        int aStart, aEnd, bStart, bEnd;
        Edit(int aStart, int aEnd, int bStart, int bEnd) {
            this.aStart = aStart; this.aEnd = aEnd; this.bStart = bStart; this.bEnd = bEnd;
        }
    }

    private static class Hunk {
        int aStart, aLen, bStart, bLen;
        List<String> lines = new ArrayList<>();
    }

    private static List<Edit> myersDiff(String[] a, String[] b) {
        int n = a.length, m = b.length;
        int max = n + m;
        int size = 2 * max + 1;
        int offset = max;
        int[] v = new int[size];
        List<int[]> trace = new ArrayList<>();
        for (int d = 0; d <= max; d++) {
            int[] vd = Arrays.copyOf(v, v.length);
            trace.add(vd);
            for (int k = -d; k <= d; k += 2) {
                int idx = k + offset;
                int x;
                if (k == -d || (k != d && v[idx - 1] < v[idx + 1])) x = v[idx + 1];
                else x = v[idx - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a[x].equals(b[y])) { x++; y++; }
                v[idx] = x;
                if (x >= n && y >= m) {
                    return backtrack(trace, a, b, d, k);
                }
            }
        }
        return new ArrayList<>();
    }

    private static List<Edit> backtrack(List<int[]> trace, String[] a, String[] b, int d, int k) {
        int n = a.length, m = b.length;
        int max = n + m, offset = max;
        List<Edit> edits = new ArrayList<>();
        int x = n, y = m;
        for (int depth = d; depth >= 0; depth--) {
            int[] v = trace.get(depth);
            int idx = k + offset;
            int prevK;
            int xPrev;
            if (k == -depth || (k != depth && v[idx - 1] < v[idx + 1])) {
                prevK = k + 1;
                xPrev = v[prevK + offset];
            } else {
                prevK = k - 1;
                xPrev = v[prevK + offset] + 1;
            }
            int yPrev = xPrev - prevK;
            while (x > xPrev && y > yPrev) { // diagonal (match)
                x--; y--;
            }
            if (depth > 0) {
                if (xPrev < x) { // deletion in a
                    edits.add(new Edit(xPrev, x, yPrev, yPrev));
                } else if (yPrev < y) { // insertion in b
                    edits.add(new Edit(xPrev, xPrev, yPrev, y));
                }
            }
            x = xPrev; y = yPrev; k = prevK;
        }
        java.util.Collections.reverse(edits);
        return edits;
    }

    private static List<Hunk> buildHunks(String[] a, String[] b, List<Edit> edits, int context) {
        List<Hunk> hunks = new ArrayList<>();
        int aIdx = 0, bIdx = 0;
        for (Edit e : edits) {
            int aStart = Math.max(e.aStart - context, 0);
            int bStart = Math.max(e.bStart - context, 0);
            int aEnd = Math.min(e.aEnd + context, a.length);
            int bEnd = Math.min(e.bEnd + context, b.length);
            Hunk h = new Hunk();
            h.aStart = aStart; h.bStart = bStart;
            h.aLen = Math.max(0, aEnd - aStart);
            h.bLen = Math.max(0, bEnd - bStart);
            // Context before
            for (int i = aStart, j = bStart; i < e.aStart && j < e.bStart; i++, j++) h.lines.add(" " + a[i]);
            // Deletions
            for (int i = e.aStart; i < e.aEnd; i++) h.lines.add("-" + a[i]);
            // Insertions
            for (int j = e.bStart; j < e.bEnd; j++) h.lines.add("+" + b[j]);
            // Context after
            for (int i = e.aEnd, j = e.bEnd; i < aEnd && j < bEnd; i++, j++) h.lines.add(" " + a[i]);
            hunks.add(h);
        }
        if (hunks.isEmpty()) {
            Hunk h = new Hunk();
            h.aStart = 0; h.bStart = 0; h.aLen = 0; h.bLen = 0; h.lines = new ArrayList<>();
            hunks.add(h);
        }
        return hunks;
    }
}