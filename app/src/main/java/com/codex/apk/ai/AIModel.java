package com.codex.apk.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum AIModel {
    // Google Models
    GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash", AIProvider.GOOGLE,
        new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
    GEMINI_2_5_PRO("gemini-2.5-pro", "Gemini 2.5 Pro", AIProvider.GOOGLE,
        new ModelCapabilities(true, true, true, true, true, true, true, 2097152, 8192)),
    GEMINI_2_0_FLASH("gemini-2.0-flash", "Gemini 2.0 Flash", AIProvider.GOOGLE,
        new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
    GEMINI_2_0_FLASH_EXP("gemini-2.0-flash-exp", "Gemini 2.0 Flash Experimental", AIProvider.GOOGLE,
        new ModelCapabilities(true, true, true, true, true, true, true, 1048576, 8192)),
    GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
    GEMINI_2_0_FLASH_THINKING("gemini-2.0-flash-thinking", "Gemini 2.0 Flash Thinking", AIProvider.GOOGLE,
        new ModelCapabilities(true, false, true, true, true, true, true, 1048576, 8192)),
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
    GEMINI_1_5_FLASH_8B("gemini-1.5-flash-8b", "Gemini 1.5 Flash 8B", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, false, false, false, 1048576, 8192)),
    GEMINI_1_5_FLASH_002("gemini-1.5-flash-002", "Gemini 1.5 Flash 002", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 8192)),
    GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
    GEMINI_1_5_PRO_002("gemini-1.5-pro-002", "Gemini 1.5 Pro 002", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, true, true, true, 2097152, 8192)),
    GEMINI_1_0_PRO("gemini-1.0-pro", "Gemini 1.0 Pro", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, false, true, false, false, false, 32768, 8192)),
    GEMINI_1_0_PRO_VISION("gemini-1.0-pro-vision", "Gemini 1.0 Pro Vision", AIProvider.GOOGLE,
        new ModelCapabilities(false, false, true, true, false, false, false, 16384, 8192)),

    // Huggingface Models
    DEEPSEEK_R1("deepseek-ai/DeepSeek-R1-Distill-Qwen-32B", "Deepseek R1", AIProvider.HUGGINGFACE,
        new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),

    // Alibaba/Qwen Models
    QWEN3_CODER_PLUS("qwen3-coder-plus", "Qwen3-Coder", AIProvider.ALIBABA,
        new ModelCapabilities(false, false, true, true, true, true, true, 1048576, 65536)),
    QWEN3_235B_A22B("qwen3-235b-a22b", "Qwen3-235B-A22B-2507", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
    QWEN3_30B_A3B("qwen3-30b-a3b", "Qwen3-30B-A3B", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
    QWEN3_32B("qwen3-32b", "Qwen3-32B", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 131072, 38912)),
    QWEN_MAX_LATEST("qwen-max-latest", "Qwen2.5-Max", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
    QWEN_PLUS_2025_01_25("qwen-plus-2025-01-25", "Qwen2.5-Plus", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 131072, 8192)),
    QWQ_32B("qwq-32b", "QwQ-32B", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, false, true, false, false, false, 131072, 8192)),
    QWEN_TURBO_2025_02_11("qwen-turbo-2025-02-11", "Qwen2.5-Turbo", AIProvider.ALIBABA,
        new ModelCapabilities(true, true, true, true, true, true, true, 1000000, 8192)),
    QWEN2_5_OMNI_7B("qwen2.5-omni-7b", "Qwen2.5-Omni-7B", AIProvider.ALIBABA,
        new ModelCapabilities(false, false, true, true, true, true, true, 30720, 2048)),
    QVQ_72B_PREVIEW("qvq-72b-preview-0310", "QVQ-Max", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
    QWEN2_5_VL_32B("qwen2.5-vl-32b-instruct", "Qwen2.5-VL-32B-Instruct", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
    QWEN2_5_14B_1M("qwen2.5-14b-instruct-1m", "Qwen2.5-14B-Instruct-1M", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, true, true, true, false, true, 1000000, 8192)),
    QWEN2_5_CODER_32B("qwen2.5-coder-32b-instruct", "Qwen2.5-Coder-32B-Instruct", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),
    QWEN2_5_72B("qwen2.5-72b-instruct", "Qwen2.5-72B-Instruct", AIProvider.ALIBABA,
        new ModelCapabilities(true, false, true, true, true, false, true, 131072, 8192)),

    // Z/GLM Models
    GLM_4_PLUS("glm-4-plus", "GLM-4-Plus", AIProvider.Z,
        new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
    GLM_4_0520("glm-4-0520", "GLM-4-0520", AIProvider.Z,
        new ModelCapabilities(true, false, true, true, false, false, true, 128000, 4096)),
    GLM_4_LONG("glm-4-long", "GLM-4-Long", AIProvider.Z,
        new ModelCapabilities(false, false, false, true, false, false, false, 1000000, 4096)),
    GLM_4_AIRX("glm-4-airx", "GLM-4-AirX", AIProvider.Z,
        new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
    GLM_4_AIR("glm-4-air", "GLM-4-Air", AIProvider.Z,
        new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
    GLM_4_FLASH("glm-4-flash", "GLM-4-Flash", AIProvider.Z,
        new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096)),
    GLM_4V_PLUS("glm-4v-plus", "GLM-4V-Plus", AIProvider.Z,
        new ModelCapabilities(true, false, true, true, true, false, true, 128000, 4096)),
    GLM_4V("glm-4v", "GLM-4V", AIProvider.Z,
        new ModelCapabilities(false, false, true, true, true, false, true, 128000, 4096)),
    COGVIEW_3_PLUS("cogview-3-plus", "CogView-3-Plus", AIProvider.Z,
        new ModelCapabilities(false, false, false, false, false, false, false, 0, 0)),
    COGVIDEOX("cogvideox", "CogVideoX", AIProvider.Z,
        new ModelCapabilities(false, false, false, false, false, true, false, 0, 0)),
    GLM_4_ALLTOOLS("glm-4-alltools", "GLM-4-AllTools", AIProvider.Z,
        new ModelCapabilities(false, false, true, true, false, false, true, 128000, 4096));

    private final String modelId;
    private final String displayName;
    private final AIProvider provider;
    private final ModelCapabilities capabilities;

    AIModel(String modelId, String displayName, AIProvider provider, ModelCapabilities capabilities) {
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

    public static List<String> getAllDisplayNames() {
        List<String> displayNames = new ArrayList<>();
        for (AIModel model : AIModel.values()) {
            displayNames.add(model.getDisplayName());
        }
        return displayNames;
    }

    public static Map<AIProvider, List<AIModel>> getModelsByProvider() {
        Map<AIProvider, List<AIModel>> groupedModels = new HashMap<>();
        for (AIProvider provider : AIProvider.values()) {
            groupedModels.put(provider, new ArrayList<>());
        }
        for (AIModel model : AIModel.values()) {
            groupedModels.get(model.getProvider()).add(model);
        }
        return groupedModels;
    }

    public static AIModel fromDisplayName(String displayName) {
        for (AIModel model : AIModel.values()) {
            if (model.getDisplayName().equals(displayName)) {
                return model;
            }
        }
        return null;
    }

    public static AIModel fromModelId(String modelId) {
        for (AIModel model : AIModel.values()) {
            if (model.getModelId().equals(modelId)) {
                return model;
            }
        }
        return null;
    }
}
