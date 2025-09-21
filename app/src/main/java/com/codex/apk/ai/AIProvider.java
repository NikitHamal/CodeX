package com.codex.apk.ai;

public enum AIProvider {
    GOOGLE("Google"),
    ALIBABA("Alibaba"),
    DEEPINFRA("DeepInfra"),
    FREE("Free"),
    COOKIES("Cookies"),
    ZHIPU("Zhipu"),
    OIVSCodeSer0501("OIVSCodeSer0501"),
    OIVSCodeSer2("OIVSCodeSer2"),
    WEWORDLE("WeWordle");

    private final String displayName;

    AIProvider(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
