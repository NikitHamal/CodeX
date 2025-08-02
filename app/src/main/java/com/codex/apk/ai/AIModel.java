package com.codex.apk.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AIModel {

    private final String modelId;
    private final String displayName;
    private final AIProvider provider;
    private final ModelCapabilities capabilities;

    // Store models in a thread-safe map, keyed by provider for efficient lookup.
    private static final Map<AIProvider, List<AIModel>> modelsByProvider = new ConcurrentHashMap<>();

    // Static initializer to populate the initial set of models
    static {
        List<AIModel> initialModels = new ArrayList<>(Arrays.asList(
            // Google Models
            new AIModel("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
            new AIModel("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
            new AIModel("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental", AIProvider.GOOGLE, new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
            new AIModel("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking", AIProvider.GOOGLE, new ModelCapabilities(true, false, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-1.5-flash", "Gemini 1.5 Flash", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
            new AIModel("gemini-1.5-flash-002", "Gemini 1.5 Flash 002", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
            new AIModel("gemini-1.5-pro", "Gemini 1.5 Pro", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
            new AIModel("gemini-1.5-pro-002", "Gemini 1.5 Pro 002", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
            new AIModel("gemini-1.0-pro", "Gemini 1.0 Pro", AIProvider.GOOGLE, new ModelCapabilities(false, false, false, true, false, false, false, 32768, 8192)),
            new AIModel("gemini-1.0-pro-vision", "Gemini 1.0 Pro Vision", AIProvider.GOOGLE, new ModelCapabilities(false, false, true, true, false, false, false, 16384, 8192)),

            // Huggingface Models
            new AIModel("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "Deepseek R1", AIProvider.HUGGINGFACE, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

            // Alibaba/Qwen Models
            new AIModel("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA, new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 65536)),
            new AIModel("qwen3-235b-a22b", "Qwen3-235B-A22B-2507", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
            new AIModel("qwen3-30b-a3b", "Qwen3-30B-A3B", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
            new AIModel("qwen3-32b", "Qwen3-32B", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
            new AIModel("qwen-max-latest", "Qwen2.5-Max", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
            new AIModel("qwen-plus-2025-01-25", "Qwen2.5-Plus", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
            new AIModel("qwq-32b", "QwQ-32B", AIProvider.ALIBABA, new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
            new AIModel("qwen-turbo-2025-02-11", "Qwen2.5-Turbo", AIProvider.ALIBABA, new ModelCapabilities(true, true, true, true, true, true, true, 1000000, 8192)),
            new AIModel("qwen2.5-omni-7b", "Qwen2.5-Omni-7B", AIProvider.ALIBABA, new ModelCapabilities(false, false, true, true, true, true, true, 30720, 2048)),
            new AIModel("qvq-72b-preview-0310", "QVQ-Max", AIProvider.ALIBABA, new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-vl-32b-instruct", "Qwen2.5-VL-32B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-14b-instruct-1m", "Qwen2.5-14B-Instruct-1M", AIProvider.ALIBABA, new ModelCapabilities(true, false, true, true, true, false, true, 1000000, 8192)),
            new AIModel("qwen2.5-coder-32b-instruct", "Qwen2.5-Coder-32B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
            new AIModel("qwen2.5-72b-instruct", "Qwen2.5-72B-Instruct", AIProvider.ALIBABA, new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),

            // Z/GLM Models
            new AIModel("glm-4-plus", "GLM-4-Plus", AIProvider.Z, new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
            new AIModel("glm-4-0520", "GLM-4-0520", AIProvider.Z, new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
            new AIModel("glm-4-long", "GLM-4-Long", AIProvider.Z, new ModelCapabilities(false, false, false, true, false, false, false, 1000000, 4096)),
            new AIModel("glm-4-airx", "GLM-4-AirX", AIProvider.Z, new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
            new AIModel("glm-4-air", "GLM-4-Air", AIProvider.Z, new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
            new AIModel("glm-4-flash", "GLM-4-Flash", AIProvider.Z, new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
            new AIModel("glm-4v-plus", "GLM-4V-Plus", AIProvider.Z, new ModelCapabilities(true, false, true, true, true, false, true, 128000, 4096)),
            new AIModel("glm-4v", "GLM-4V", AIProvider.Z, new ModelCapabilities(false, false, true, true, true, false, true, 128000, 4096)),
            new AIModel("cogview-3-plus", "CogView-3-Plus", AIProvider.Z, new ModelCapabilities(false, false, false, false, false, false, false, 0, 0)),
            new AIModel("cogvideox", "CogVideoX", AIProvider.Z, new ModelCapabilities(false, false, false, false, false, true, false, 0, 0)),
            new AIModel("glm-4-alltools", "GLM-4-AllTools", AIProvider.Z, new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096))
        ));
        for (AIModel model : initialModels) {
            modelsByProvider.computeIfAbsent(model.getProvider(), k -> new ArrayList<>()).add(model);
        }
    }

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

    public static List<AIModel> values() {
        List<AIModel> allModels = new ArrayList<>();
        for (List<AIModel> modelList : modelsByProvider.values()) {
            allModels.addAll(modelList);
        }
        return allModels;
    }

    public static List<String> getAllDisplayNames() {
        List<String> displayNames = new ArrayList<>();
        for (AIModel model : values()) {
            displayNames.add(model.getDisplayName());
        }
        return displayNames;
    }

    public static Map<AIProvider, List<AIModel>> getModelsByProvider() {
        return new HashMap<>(modelsByProvider); // Return a copy
    }

    public static void updateModelsForProvider(AIProvider provider, List<AIModel> newModels) {
        modelsByProvider.put(provider, new ArrayList<>(newModels));
    }

    public static AIModel fromDisplayName(String displayName) {
        if (displayName == null) return null;
        for (AIModel model : values()) {
            if (displayName.equals(model.getDisplayName())) {
                return model;
            }
        }
        return null;
    }

    public static AIModel fromModelId(String modelId) {
        if (modelId == null) return null;
        for (AIModel model : values()) {
            if (modelId.equals(model.getModelId())) {
                return model;
            }
        }
        return null;
    }
}
