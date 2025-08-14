package com.codex.apk;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared utilities for unified diff parsing and metrics.
 */
public final class DiffUtils {

    private DiffUtils() {}

    public enum LineType { ADDED, REMOVED, CONTEXT, HEADER }

    public static class DiffLine {
        public final LineType type;
        public final Integer oldLine; // nullable
        public final Integer newLine; // nullable
        public final String text;

        public DiffLine(LineType type, Integer oldLine, Integer newLine, String text) {
            this.type = type;
            this.oldLine = oldLine;
            this.newLine = newLine;
            this.text = text == null ? "" : text;
        }
    }

    /**
     * Parse a unified diff string into a sequence of DiffLine entries.
     */
    public static List<DiffLine> parseUnifiedDiff(String diffText) {
        List<DiffLine> out = new ArrayList<>();
        if (diffText == null || diffText.isEmpty()) return out;

        String[] rows = diffText.split("\n", -1);
        int oldLine = 0;
        int newLine = 0;
        boolean haveHeader = false;
        for (String raw : rows) {
            if (raw.startsWith("@@")) {
                haveHeader = true;
                try {
                    int minusIdx = raw.indexOf('-');
                    int plusIdx = raw.indexOf('+');
                    int at2 = raw.indexOf("@@", 2);
                    if (minusIdx != -1 && plusIdx != -1 && at2 != -1) {
                        String aPart = raw.substring(minusIdx + 1, raw.indexOf(' ', minusIdx + 1)).trim();
                        String bPart = raw.substring(plusIdx + 1, raw.indexOf(' ', plusIdx + 1)).trim();
                        String[] aSplit = aPart.split(",");
                        String[] bSplit = bPart.split(",");
                        oldLine = Integer.parseInt(aSplit[0]);
                        newLine = Integer.parseInt(bSplit[0]);
                    }
                } catch (Throwable ignore) {}
                out.add(new DiffLine(LineType.HEADER, null, null, raw));
                continue;
            }
            if (raw.startsWith("--- ") || raw.startsWith("+++ ")) {
                out.add(new DiffLine(LineType.HEADER, null, null, raw));
                continue;
            }
            if (!haveHeader) {
                out.add(new DiffLine(LineType.HEADER, null, null, raw));
                continue;
            }

            if (raw.startsWith("+")) {
                String text = raw.length() > 1 ? raw.substring(1) : "";
                out.add(new DiffLine(LineType.ADDED, null, newLine, text));
                newLine++;
            } else if (raw.startsWith("-")) {
                String text = raw.length() > 1 ? raw.substring(1) : "";
                out.add(new DiffLine(LineType.REMOVED, oldLine, null, text));
                oldLine++;
            } else if (raw.startsWith(" ")) {
                String text = raw.length() > 1 ? raw.substring(1) : "";
                out.add(new DiffLine(LineType.CONTEXT, oldLine, newLine, text));
                oldLine++;
                newLine++;
            } else {
                out.add(new DiffLine(LineType.HEADER, null, null, raw));
            }
        }
        return out;
    }

    /**
     * Count added and removed lines in a unified diff.
     * Returns int[]{added, removed}
     */
    public static int[] countAddRemove(String patch) {
        int adds = 0;
        int rems = 0;
        if (patch == null || patch.isEmpty()) return new int[]{0, 0};
        try {
            String[] lines = patch.split("\n", -1);
            for (String line : lines) {
                if (line.isEmpty()) continue;
                if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("@@")) continue;
                char c = line.charAt(0);
                if (c == '+') adds++;
                else if (c == '-') rems++;
            }
        } catch (Throwable ignore) {}
        return new int[]{adds, rems};
    }

    /**
     * Compute added and removed line counts between old and new contents.
     * Returns int[]{added, removed}
     */
    public static int[] countAddRemoveFromContents(String oldContent, String newContent) {
        if (oldContent == null) oldContent = "";
        if (newContent == null) newContent = "";
        String[] a = oldContent.split("\n", -1);
        String[] b = newContent.split("\n", -1);
        int n = a.length, m = b.length;
        // LCS DP
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a[i - 1].equals(b[j - 1])) dp[i][j] = dp[i - 1][j - 1] + 1;
                else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
        int lcs = dp[n][m];
        int removed = n - lcs;
        int added = m - lcs;
        return new int[]{added, removed};
    }
}
