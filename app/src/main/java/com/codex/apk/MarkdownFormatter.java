package com.codex.apk;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import androidx.annotation.NonNull;

import io.noties.markwon.core.Markwon;
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.ext.tasklist.TaskListPlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.linkify.LinkifyPlugin;
import io.noties.markwon.syntax.Prism4jTheme;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;
import io.noties.prism4j.annotations.PrismBundle;

@PrismBundle(includeAll = true)
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
                .usePlugin(SyntaxHighlightPlugin.create(new Prism4j(), createCodeTheme()))
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
     * Creates a dark theme for code syntax highlighting
     */
    private Prism4jTheme createCodeTheme() {
        return Prism4jTheme.create();
    }
    
    /**
     * Preprocesses markdown content to handle special formatting cases
     */
    public String preprocessMarkdown(String markdown) {
        if (markdown == null) return "";
        
        // Handle code blocks with language specification
        markdown = markdown.replaceAll("```(\\w+)\\n", "```$1\n");
        
        // Ensure proper line breaks for lists
        markdown = markdown.replaceAll("(?<!\\n)\\n([*+-]\\s)", "\n\n$1");
        
        // Handle numbered lists
        markdown = markdown.replaceAll("(?<!\\n)\\n(\\d+\\.\\s)", "\n\n$1");
        
        return markdown;
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