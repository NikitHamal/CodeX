package com.codex.apk.ai;

public enum AIProvider {
    GOOGLE("Google"),
    HUGGINGFACE("Huggingface"),
    ALIBABA("Alibaba"),
    Z("Z");

    private final String displayName;

    AIProvider(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
