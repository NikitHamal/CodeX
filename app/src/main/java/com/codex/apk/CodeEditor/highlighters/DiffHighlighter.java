package com.codex.apk.CodeEditor.highlighters;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.codex.apk.CodeEditor.SyntaxColorProvider;
import com.codex.apk.R; // Ensure R is imported correctly for colors

/**
 * Handles highlighting for diff content.
 */
public class DiffHighlighter {

    private final SyntaxColorProvider colorProvider;

    public DiffHighlighter(SyntaxColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    /**
     * Applies diff highlighting to the given Editable text based on unified diff prefixes.
     * This method removes all existing ForegroundColorSpans before applying new ones.
     *
     * @param editable The Editable text representing a unified diff.
     */
    public void highlight(Editable editable) {
        if (editable == null) return;

        // Remove all existing spans (including any syntax highlighting if previously applied)
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }

        String text = editable.toString();
        String[] lines = text.split("\\r?\\n");
        int currentOffset = 0;

        for (String line : lines) {
            int lineColor = ContextCompat.getColor(colorProvider.getContext(), R.color.on_surface); // Default text color

            if (line.startsWith("+")) {
                lineColor = colorProvider.getDiffAddedColor();
            } else if (line.startsWith("-")) {
                lineColor = colorProvider.getDiffDeletedColor();
            } else if (line.startsWith(" ")) {
                lineColor = colorProvider.getDiffUnchangedColor();
            } else if (line.startsWith("diff") || line.startsWith("---") || line.startsWith("+++") || line.startsWith("@@")) {
                // Diff headers and hunk headers
                lineColor = colorProvider.getDiffUnchangedColor(); // Or a specific header color
            }

            // Apply span to the entire line
            editable.setSpan(new ForegroundColorSpan(lineColor), currentOffset, currentOffset + line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            currentOffset += line.length() + (text.length() > currentOffset + line.length() ? 1 : 0); // +1 for newline if it exists
        }
    }
}
