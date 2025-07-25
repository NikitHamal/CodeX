package com.codex.apk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for generating diffs between two strings.
 * Provides a basic unified diff format.
 */
public class DiffUtils {

    /**
     * Generates a unified diff string between two pieces of text.
     * This is a simplified implementation and might not be as robust as professional diff tools.
     * It performs a line-by-line comparison.
     *
     * @param oldContent The original text.
     * @param newContent The new text.
     * @return A string representing the unified diff.
     */
    public static String generateUnifiedDiff(String oldContent, String newContent) {
        List<String> oldLines = new ArrayList<>(Arrays.asList(oldContent.split("\\r?\\n")));
        List<String> newLines = new ArrayList<>(Arrays.asList(newContent.split("\\r?\\n")));

        // Add an empty string if content is empty to ensure at least one line for split
        if (oldContent.isEmpty()) oldLines.add("");
        if (newContent.isEmpty()) newLines.add("");


        StringBuilder diff = new StringBuilder();
        int oldIdx = 0;
        int newIdx = 0;

        while (oldIdx < oldLines.size() || newIdx < newLines.size()) {
            if (oldIdx < oldLines.size() && newIdx < newLines.size() && oldLines.get(oldIdx).equals(newLines.get(newIdx))) {
                // Unchanged line
                diff.append("  ").append(oldLines.get(oldIdx)).append("\n");
                oldIdx++;
                newIdx++;
            } else {
                boolean oldLineConsumed = false;
                boolean newLineConsumed = false;

                // Look for deletions
                if (oldIdx < oldLines.size()) {
                    // Check if the current old line is present later in new lines
                    boolean foundInNew = false;
                    for (int i = newIdx; i < newLines.size(); i++) {
                        if (oldLines.get(oldIdx).equals(newLines.get(i))) {
                            foundInNew = true;
                            break;
                        }
                    }
                    if (!foundInNew) {
                        diff.append("- ").append(oldLines.get(oldIdx)).append("\n");
                        oldIdx++;
                        oldLineConsumed = true;
                    }
                }

                // Look for additions
                if (newIdx < newLines.size()) {
                    // Check if the current new line is present later in old lines
                    boolean foundInOld = false;
                    for (int i = oldIdx; i < oldLines.size(); i++) {
                        if (newLines.get(newIdx).equals(oldLines.get(i))) {
                            foundInOld = true;
                            break;
                        }
                    }
                    if (!foundInOld) {
                        diff.append("+ ").append(newLines.get(newIdx)).append("\n");
                        newIdx++;
                        newLineConsumed = true;
                    }
                }

                // If neither line was consumed, it means they are different and neither is a clear addition/deletion
                // This implies a modification (replacement). We'll show it as a deletion followed by an addition.
                if (!oldLineConsumed && !newLineConsumed && oldIdx < oldLines.size() && newIdx < newLines.size()) {
                    diff.append("- ").append(oldLines.get(oldIdx)).append("\n");
                    diff.append("+ ").append(newLines.get(newIdx)).append("\n");
                    oldIdx++;
                    newIdx++;
                }
            }
        }
        return diff.toString();
    }
}
