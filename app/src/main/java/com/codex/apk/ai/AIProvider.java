package com.codex.apk.ai;

public enum AIProvider {
    GOOGLE("Google"),
    ALIBABA("Alibaba"),
    Z("Z"),
    FREE("Free"),
    COOKIES("Cookies"),
    GPT_OSS("GPT OSS");

    private final String displayName;

    AIProvider(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
