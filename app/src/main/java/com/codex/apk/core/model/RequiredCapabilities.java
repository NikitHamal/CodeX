package com.codex.apk.core.model;

/**
 * Required capabilities for AI requests.
 */
public class RequiredCapabilities {
    private final boolean streaming;
    private final boolean vision;
    private final boolean tools;
    private final boolean webSearch;
    private final boolean thinking;
    private final boolean multimodal;

    public RequiredCapabilities() {
        this(false, false, false, false, false, false);
    }

    public RequiredCapabilities(boolean streaming, boolean vision, boolean tools,
                               boolean webSearch, boolean thinking, boolean multimodal) {
        this.streaming = streaming;
        this.vision = vision;
        this.tools = tools;
        this.webSearch = webSearch;
        this.thinking = thinking;
        this.multimodal = multimodal;
    }

    public boolean requires(String capability) {
        switch (capability.toLowerCase()) {
            case "streaming": return streaming;
            case "vision": return vision;
            case "tools": return tools;
            case "websearch": return webSearch;
            case "thinking": return thinking;
            case "multimodal": return multimodal;
            default: return false;
        }
    }

    public boolean isCompatibleWith(ProviderCapabilities provider) {
        return (!streaming || provider.supportsStreaming()) &&
               (!vision || provider.supportsVision()) &&
               (!tools || provider.supportsTools()) &&
               (!webSearch || provider.supportsWebSearch()) &&
               (!thinking || provider.supportsThinking()) &&
               (!multimodal || provider.supportsMultimodal());
    }
}
