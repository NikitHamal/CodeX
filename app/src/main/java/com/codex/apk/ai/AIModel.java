package com.codex.apk.ai;

public class AIModel {

    private final String modelId;
    private final String displayName;
    private final AIProvider provider;
    private final ModelCapabilities capabilities;

    public AIModel(String modelId, String displayName, AIProvider provider, ModelCapabilities capabilities) {
        this.modelId = modelId;
        this.displayName = displayName;
        this.provider = provider;
        this.capabilities = capabilities;
    }

    public String getModelId() { return modelId; }
    public String getDisplayName() { return displayName; }
    public AIProvider getProvider() { return provider; }
    public ModelCapabilities getCapabilities() { return capabilities; }
    public boolean supportsVision() { return capabilities.supportsVision; }
    public boolean supportsFunctionCalling() { return true; }
}
