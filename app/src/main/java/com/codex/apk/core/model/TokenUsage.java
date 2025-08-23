package com.codex.apk.core.model;

/**
 * Token usage information.
 */
public class TokenUsage {
    private final int promptTokens;
    private final int completionTokens;
    private final int totalTokens;

    public TokenUsage(int promptTokens, int completionTokens, int totalTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public int getTotalTokens() { return totalTokens; }
}
