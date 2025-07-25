package com.codex.apk.CodeEditor;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for syntax highlighting operations.
 */
public class SyntaxHighlightingUtils {

    /**
     * Applies a ForegroundColorSpan to all occurrences of a pattern within a SpannableStringBuilder.
     * This method is a utility for general pattern highlighting.
     * @param spannable The SpannableStringBuilder to apply highlighting to.
     * @param pattern The regex pattern to match.
     * @param color The color to apply.
     */
    public static void highlightPattern(SpannableStringBuilder spannable, Pattern pattern, int color) {
        String text = spannable.toString();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            spannable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Extracts the file extension from a given file name.
     * @param fileName The name of the file.
     * @return The file extension (e.g., "java", "xml", "html"), or an empty string if no extension is found.
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Detects syntax type from file extension.
     * Only detects for HTML, CSS, JAVASCRIPT, JSON, and DIFF.
     * For any other extension, it will default to HTML.
     * @param fileName The name of the file.
     * @return The detected SyntaxType.
     */
    public static OptimizedSyntaxHighlighter.SyntaxType detectSyntaxType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();

        switch (extension) {
            case "html":
            case "htm":
                return OptimizedSyntaxHighlighter.SyntaxType.HTML;
            case "css":
                return OptimizedSyntaxHighlighter.SyntaxType.CSS;
            case "js":
                return OptimizedSyntaxHighlighter.SyntaxType.JAVASCRIPT;
            case "json":
                return OptimizedSyntaxHighlighter.SyntaxType.JSON;
            case "diff": // Explicitly handle .diff files if they exist
                return OptimizedSyntaxHighlighter.SyntaxType.DIFF;
            default:
                // Default to HTML for unsupported or unknown types, as per enhancement request
                return OptimizedSyntaxHighlighter.SyntaxType.HTML;
        }
    }
}
