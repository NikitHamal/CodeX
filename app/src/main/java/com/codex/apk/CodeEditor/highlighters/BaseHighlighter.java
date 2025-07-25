package com.codex.apk.CodeEditor.highlighters;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.codex.apk.CodeEditor.SyntaxColorProvider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for syntax highlighters, providing common utility methods.
 */
public abstract class BaseHighlighter {

    protected final SyntaxColorProvider colorProvider;

    public BaseHighlighter(SyntaxColorProvider colorProvider) {
        this.colorProvider = colorProvider;
    }

    /**
     * Applies a ForegroundColorSpan to all occurrences of a pattern within a SpannableStringBuilder.
     * @param spannable The SpannableStringBuilder to apply highlighting to.
     * @param pattern The regex pattern to match.
     * @param colorIndex The index in the colorProvider's syntaxColors array for the color to apply.
     */
    protected void highlightPattern(SpannableStringBuilder spannable, Pattern pattern, int colorIndex) {
        String text = spannable.toString();
        Matcher matcher = pattern.matcher(text);
        int color = colorProvider.getSyntaxColors()[colorIndex];
        while (matcher.find()) {
            spannable.setSpan(
                    new ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * Abstract method to be implemented by concrete highlighter classes to perform language-specific highlighting.
     * @param spannable The SpannableStringBuilder containing the text to highlight.
     */
    public abstract void highlight(SpannableStringBuilder spannable);
}
