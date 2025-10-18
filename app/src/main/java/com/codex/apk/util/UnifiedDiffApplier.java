package com.codex.apk.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UnifiedDiffApplier {
    private UnifiedDiffApplier() {}

    public static String apply(String original, String patch) {
        List<String> source = new ArrayList<>(Arrays.asList(original.split("\n", -1)));
        List<Hunk> hunks = parseHunks(patch);
        int offset = 0;
        for (Hunk hunk : hunks) {
            int expectedIndex = Math.max(0, Math.min(source.size(), hunk.startOld - 1 + offset));
            int matchedIndex = findBestMatch(source, hunk, expectedIndex);
            if (matchedIndex < 0) {
                continue;
            }

            int removeCount = Math.min(hunk.lenOld, Math.max(0, source.size() - matchedIndex));
            for (int i = 0; i < removeCount; i++) {
                if (matchedIndex < source.size()) {
                    source.remove(matchedIndex);
                }
            }

            List<String> toInsert = new ArrayList<>();
            for (String line : hunk.lines) {
                if (line.startsWith(" ") || line.startsWith("+")) {
                    toInsert.add(line.length() > 0 ? line.substring(1) : "");
                }
            }
            source.addAll(matchedIndex, toInsert);
            offset += toInsert.size() - removeCount;
        }
        return String.join("\n", source);
    }

    private static int findBestMatch(List<String> source, Hunk hunk, int expectedIndex) {
        if (contextMatches(source, expectedIndex, hunk)) {
            return expectedIndex;
        }
        int window = 50;
        for (int delta = 1; delta <= window; delta++) {
            int left = expectedIndex - delta;
            int right = expectedIndex + delta;
            if (left >= 0 && contextMatches(source, left, hunk)) {
                return left;
            }
            if (right <= source.size() && contextMatches(source, right, hunk)) {
                return right;
            }
        }
        return -1;
    }

    private static boolean contextMatches(List<String> source, int index, Hunk hunk) {
        int cursor = index;
        for (String line : hunk.lines) {
            if (line.startsWith(" ") || line.startsWith("-")) {
                String expected = line.length() > 0 ? line.substring(1) : "";
                if (cursor >= source.size()) return false;
                if (!source.get(cursor).equals(expected)) return false;
                cursor++;
            }
        }
        return true;
    }

    private static List<Hunk> parseHunks(String patch) {
        List<Hunk> hunks = new ArrayList<>();
        String[] lines = patch.split("\n");
        Hunk current = null;
        for (String line : lines) {
            if (line.startsWith("@@")) {
                if (current != null) {
                    hunks.add(current);
                }
                current = new Hunk();
                int[] header = parseHeader(line);
                current.startOld = header[0];
                current.lenOld = header[1];
                current.startNew = header[2];
                current.lenNew = header[3];
            } else if (current != null && (line.startsWith(" ") || line.startsWith("+") || line.startsWith("-"))) {
                current.lines.add(line);
            }
        }
        if (current != null) {
            hunks.add(current);
        }
        return hunks;
    }

    private static int[] parseHeader(String header) {
        try {
            String core = header.substring(2, header.indexOf("@@", 2)).trim();
            String[] parts = core.split(" ");
            String[] oldPart = parts[0].substring(1).split(",");
            String[] newPart = parts[1].substring(1).split(",");
            int startOld = parseIntSafe(oldPart[0], 1);
            int lenOld = oldPart.length > 1 ? parseIntSafe(oldPart[1], 0) : 0;
            int startNew = parseIntSafe(newPart[0], 1);
            int lenNew = newPart.length > 1 ? parseIntSafe(newPart[1], 0) : 0;
            return new int[]{startOld, lenOld, startNew, lenNew};
        } catch (Exception ex) {
            return new int[]{1, 0, 1, 0};
        }
    }

    private static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static final class Hunk {
        int startOld;
        int lenOld;
        int startNew;
        int lenNew;
        final List<String> lines = new ArrayList<>();
    }
}
