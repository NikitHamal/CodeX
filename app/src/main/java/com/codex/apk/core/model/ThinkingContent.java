package com.codex.apk.core.model;

/**
 * Thinking content for models that expose reasoning.
 */
public class ThinkingContent {
    private final String content;
    private final boolean isVisible;

    public ThinkingContent(String content, boolean isVisible) {
        this.content = content;
        this.isVisible = isVisible;
    }

    public String getContent() { return content; }
    public boolean isVisible() { return isVisible; }
    public boolean hasContent() { return content != null && !content.trim().isEmpty(); }
}
