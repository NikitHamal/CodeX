package com.codex.apk;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;

public class MarkdownFormatter {
    
    private static MarkdownFormatter instance;
    private final Markwon markwon;
    private final Markwon thinkingMarkwon;
    
    private MarkdownFormatter(Context context) {
        // Create main markwon instance for AI messages
        markwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(ImagesPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .build();
        
        // Create simplified markwon instance for thinking content (no images, simpler formatting)
        thinkingMarkwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(LinkifyPlugin.create())
                .build();
    }
    
    public static synchronized MarkdownFormatter getInstance(Context context) {
        if (instance == null) {
            instance = new MarkdownFormatter(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Formats and sets markdown content to a TextView for AI messages
     */
    public void setMarkdown(@NonNull TextView textView, @NonNull String markdown) {
        markwon.setMarkdown(textView, markdown);
    }
    
    /**
     * Formats and sets markdown content to a TextView for thinking content
     */
    public void setThinkingMarkdown(@NonNull TextView textView, @NonNull String markdown) {
        thinkingMarkwon.setMarkdown(textView, markdown);
    }
    
    /**
     * Converts markdown to spanned text without setting it to a view
     */
    public CharSequence toMarkdown(@NonNull String markdown) {
        return markwon.toMarkdown(markdown);
    }
    
    /**
     * Converts thinking markdown to spanned text
     */
    public CharSequence toThinkingMarkdown(@NonNull String markdown) {
        return thinkingMarkwon.toMarkdown(markdown);
    }
    
    /**
     * Preprocesses markdown content to handle special formatting cases
     */
    public String preprocessMarkdown(String markdown) {
        if (markdown == null) return "";
        String s = markdown;

        // Heuristic: if looks like raw/minified HTML and not already fenced, wrap and pretty-print
        String trimmed = s.trim();
        boolean alreadyFenced = trimmed.contains("```");
        boolean looksHtml = !alreadyFenced && trimmed.startsWith("<") && trimmed.contains(">") && (
                trimmed.contains("<html") || trimmed.contains("<!DOCTYPE") || trimmed.contains("<head") ||
                trimmed.contains("<body") || trimmed.contains("<div") || trimmed.contains("<span") ||
                trimmed.contains("<p ") || trimmed.contains("<p>") || trimmed.matches("(?s).*<([a-zA-Z][a-zA-Z0-9-]*)([^>]*)>.*</\\1>.*")
        );
        if (looksHtml) {
            // Insert newlines between adjacent tags to improve readability
            String pretty = trimmed.replace("><", ">\n<");
            return "```html\n" + pretty + "\n```";
        }

        // Handle code blocks with language specification (normalize line endings after language)
        s = s.replaceAll("```(\\w+)\\r?\\n", "```$1\n");

        // Ensure proper line breaks for lists
        s = s.replaceAll("(?<!\\n)\\n([*+-]\\s)", "\n\n$1");

        // Handle numbered lists
        s = s.replaceAll("(?<!\\n)\\n(\\d+\\.\\s)", "\n\n$1");

        // Normalize citations spacing: [[n]] -> [[n]] with surrounding spaces ensured by renderer
        s = s.replaceAll("\\[\\[(\\d+)\\]\\]", "[[$1]]");

        return s;
    }
    
    /**
     * Extracts code blocks from markdown for special handling
     */
    public boolean containsCodeBlocks(String markdown) {
        return markdown != null && markdown.contains("```");
    }
    
    /**
     * Checks if the markdown contains tables
     */
    public boolean containsTables(String markdown) {
        return markdown != null && markdown.contains("|");
    }
}