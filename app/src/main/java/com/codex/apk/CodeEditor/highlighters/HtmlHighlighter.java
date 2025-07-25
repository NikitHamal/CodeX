package com.codex.apk.CodeEditor.highlighters;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.codex.apk.CodeEditor.SyntaxColorProvider;
import com.codex.apk.CodeEditor.patterns.HtmlPatterns;

import java.util.regex.Matcher;

/**
 * Handles syntax highlighting for HTML.
 * Enhanced to better distinguish tags, attributes, values, comments, and entities.
 */
public class HtmlHighlighter extends BaseHighlighter {

    public HtmlHighlighter(SyntaxColorProvider colorProvider) {
        super(colorProvider);
    }

    @Override
    public void highlight(SpannableStringBuilder spannable) {
        // Doctype declaration
        highlightPattern(spannable, HtmlPatterns.HTML_DOCTYPE_PATTERN, 4);
        // HTML Comments
        highlightPattern(spannable, HtmlPatterns.HTML_COMMENT_PATTERN, 3);
        // HTML Tags (e.g., <div>, <p>)
        highlightPattern(spannable, HtmlPatterns.HTML_TAG_NAME_PATTERN, 0); // Highlight tag names specifically
        // Attributes and their values within tags
        highlightHtmlAttributesAndValues(spannable);
        // HTML Entities (e.g., &nbsp;, &amp;)
        highlightPattern(spannable, HtmlPatterns.HTML_ENTITY_PATTERN, 5);
    }

    /**
     * Highlights HTML attributes and their corresponding values.
     * This method iterates through HTML tags and then finds attributes and values within those tags.
     */
    private void highlightHtmlAttributesAndValues(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        Matcher tagMatcher = HtmlPatterns.HTML_FULL_TAG_PATTERN.matcher(text); // Use a pattern to find the full tag content
        int[] colors = colorProvider.getSyntaxColors();

        int attrColor = colors[1]; // Color for attributes
        int valueColor = colors[2]; // Color for values (strings)

        while (tagMatcher.find()) {
            String fullTagContent = tagMatcher.group();
            int tagStartGlobalOffset = tagMatcher.start();

            // Find attributes within this full tag content
            Matcher attrMatcher = HtmlPatterns.HTML_ATTRIBUTE_NAME_PATTERN.matcher(fullTagContent);
            while (attrMatcher.find()) {
                // Adjust start and end offsets relative to the global document
                int attrStart = tagStartGlobalOffset + attrMatcher.start(1); // Group 1 is the attribute name
                int attrEnd = tagStartGlobalOffset + attrMatcher.end(1);
                spannable.setSpan(
                        new ForegroundColorSpan(attrColor),
                        attrStart,
                        attrEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Find attribute values (quoted strings) within this full tag content
            Matcher valueMatcher = HtmlPatterns.HTML_ATTRIBUTE_VALUE_PATTERN.matcher(fullTagContent);
            while (valueMatcher.find()) {
                // Adjust start and end offsets relative to the global document
                int valueStart = tagStartGlobalOffset + valueMatcher.start(1); // Group 1 is the content inside quotes
                int valueEnd = tagStartGlobalOffset + valueMatcher.end(1);
                spannable.setSpan(
                        new ForegroundColorSpan(valueColor),
                        valueStart,
                        valueEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }
}
