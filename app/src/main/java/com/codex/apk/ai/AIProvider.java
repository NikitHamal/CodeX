package com.codex.apk.ai;

public enum AIProvider {
    GOOGLE("Google"),
    ALIBABA("Alibaba"),
    DEEPINFRA("DeepInfra"),
    FREE("Free"),
    COOKIES("Cookies"),
    OIVSCodeSer0501("OIVSCodeSer0501"),
    WEWORDLE("WeWordle"),
    OPENROUTER("OpenRouter");

    private final String displayName;

    AIProvider(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
