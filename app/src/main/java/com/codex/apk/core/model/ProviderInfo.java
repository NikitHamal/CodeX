package com.codex.apk.core.model;

/**
 * Provider information.
 */
public class ProviderInfo {
    private final com.codex.apk.ai.AIProvider type;
    private final String displayName;
    private final String description;
    private final ProviderCapabilities capabilities;

    public ProviderInfo(com.codex.apk.ai.AIProvider type, String displayName, String description, ProviderCapabilities capabilities) {
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.capabilities = capabilities;
    }

    public com.codex.apk.ai.AIProvider getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ProviderCapabilities getCapabilities() { return capabilities; }
}
