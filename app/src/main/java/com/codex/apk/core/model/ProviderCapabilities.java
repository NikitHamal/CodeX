package com.codex.apk.core.model;

/**
 * Provider capabilities model.
 */
public class ProviderCapabilities {
    private final boolean supportsStreaming;
    private final boolean supportsVision;
    private final boolean supportsTools;
    private final boolean supportsWebSearch;
    private final boolean supportsThinking;
    private final boolean supportsMultimodal;
    private final int maxTokens;
    private final java.util.Set<String> supportedFormats;

    public ProviderCapabilities(boolean supportsStreaming, boolean supportsVision, boolean supportsTools,
                               boolean supportsWebSearch, boolean supportsThinking, boolean supportsMultimodal,
                               int maxTokens, java.util.Set<String> supportedFormats) {
        this.supportsStreaming = supportsStreaming;
        this.supportsVision = supportsVision;
        this.supportsTools = supportsTools;
        this.supportsWebSearch = supportsWebSearch;
        this.supportsThinking = supportsThinking;
        this.supportsMultimodal = supportsMultimodal;
        this.maxTokens = maxTokens;
        this.supportedFormats = supportedFormats;
    }

    public boolean supportsStreaming() { return supportsStreaming; }
    public boolean supportsVision() { return supportsVision; }
    public boolean supportsTools() { return supportsTools; }
    public boolean supportsWebSearch() { return supportsWebSearch; }
    public boolean supportsThinking() { return supportsThinking; }
    public boolean supportsMultimodal() { return supportsMultimodal; }
    public int getMaxTokens() { return maxTokens; }
    public java.util.Set<String> getSupportedFormats() { return supportedFormats; }

    public boolean canHandle(AIRequest request) {
        RequiredCapabilities required = request.getRequiredCapabilities();
        return required.isCompatibleWith(this);
    }

    public AIRequest optimizeRequest(AIRequest request) {
        // Basic optimization - could be extended per provider
        return request;
    }
}
