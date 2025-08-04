package com.codex.apk;

import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import com.codex.apk.ai.ModelCapabilities;
import com.codex.apk.ai.AIModel;

/**
 * Utility class for comparing and analyzing Alibaba model capabilities.
 * Helps understand the differences between old and new model features.
 */
public class ModelComparisonUtil {
    private static final String TAG = "ModelComparisonUtil";

    /**
     * Analyze and compare model capabilities
     */
    public static void analyzeModels(List<AIModel> models) {
        Log.i(TAG, "=== Alibaba Model Analysis ===");
        Log.i(TAG, "Total models found: " + models.size());
        
        // Group models by type
        Map<String, List<AIModel>> modelGroups = new HashMap<>();
        for (AIModel model : models) {
            String group = getModelGroup(model.getDisplayName());
            modelGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(model);
        }
        
        // Analyze each group
        for (Map.Entry<String, List<AIModel>> entry : modelGroups.entrySet()) {
            Log.i(TAG, "\n=== " + entry.getKey() + " Models ===");
            for (AIModel model : entry.getValue()) {
                analyzeModel(model);
            }
        }
        
        // Find new features
        findNewFeatures(models);
    }

    /**
     * Analyze a single model
     */
    public static void analyzeModel(AIModel model) {
        ModelCapabilities caps = model.getCapabilities();
        
        Log.i(TAG, "\nModel: " + model.getDisplayName());
        Log.i(TAG, "ID: " + model.getModelId());
        Log.i(TAG, "Context Length: " + caps.getContextLengthDisplay());
        Log.i(TAG, "Capabilities: " + caps.getCapabilitySummary());
        
        // Check for new features
        if (caps.supportsThinkingBudget) {
            Log.i(TAG, "✨ NEW: Supports Thinking Budget");
        }
        if (caps.supportsMCP) {
            Log.i(TAG, "✨ NEW: Supports MCP Tools: " + caps.mcpTools);
        }
        if (caps.isSingleRound) {
            Log.i(TAG, "✨ NEW: Single Round Conversation");
        }
        if (!caps.supportedModalities.isEmpty()) {
            Log.i(TAG, "✨ NEW: Modalities: " + caps.supportedModalities);
        }
        if (!caps.fileLimits.isEmpty()) {
            Log.i(TAG, "✨ NEW: File Limits: " + caps.fileLimits);
        }
        if (!caps.abilities.isEmpty()) {
            Log.i(TAG, "✨ NEW: Ability Levels: " + caps.abilities);
        }
    }

    /**
     * Group models by type
     */
    private static String getModelGroup(String displayName) {
        if (displayName.contains("Qwen3")) {
            return "Qwen3 Series";
        } else if (displayName.contains("Qwen2.5")) {
            return "Qwen2.5 Series";
        } else if (displayName.contains("Coder")) {
            return "Coder Models";
        } else if (displayName.contains("VL") || displayName.contains("QVQ")) {
            return "Vision Models";
        } else if (displayName.contains("Omni")) {
            return "Omni Models";
        } else {
            return "Other Models";
        }
    }

    /**
     * Find new features across all models
     */
    private static void findNewFeatures(List<AIModel> models) {
        Log.i(TAG, "\n=== New Features Analysis ===");
        
        boolean hasThinkingBudget = false;
        boolean hasMCP = false;
        boolean hasSingleRound = false;
        boolean hasFileLimits = false;
        boolean hasAbilities = false;
        boolean hasOmniModality = false;
        
        for (AIModel model : models) {
            ModelCapabilities caps = model.getCapabilities();
            
            if (caps.supportsThinkingBudget) hasThinkingBudget = true;
            if (caps.supportsMCP) hasMCP = true;
            if (caps.isSingleRound) hasSingleRound = true;
            if (!caps.fileLimits.isEmpty()) hasFileLimits = true;
            if (!caps.abilities.isEmpty()) hasAbilities = true;
            if (caps.supportsModality("audio") || caps.supportsModality("video")) hasOmniModality = true;
        }
        
        Log.i(TAG, "New Features Found:");
        if (hasThinkingBudget) Log.i(TAG, "✅ Thinking Budget Support");
        if (hasMCP) Log.i(TAG, "✅ MCP (Model Context Protocol) Support");
        if (hasSingleRound) Log.i(TAG, "✅ Single Round Conversations");
        if (hasFileLimits) Log.i(TAG, "✅ File Size/Count Limits");
        if (hasAbilities) Log.i(TAG, "✅ Numeric Ability Levels");
        if (hasOmniModality) Log.i(TAG, "✅ Multi-Modal Support (Audio/Video)");
    }

    /**
     * Get model recommendations based on use case
     */
    public static List<AIModel> getRecommendedModels(List<AIModel> models, String useCase) {
        List<AIModel> recommendations = new ArrayList<>();
        
        switch (useCase.toLowerCase()) {
            case "coding":
                for (AIModel model : models) {
                    if (model.getDisplayName().contains("Coder") || 
                        model.getDisplayName().contains("Code")) {
                        recommendations.add(model);
                    }
                }
                break;
                
            case "vision":
                for (AIModel model : models) {
                    if (model.getCapabilities().supportsVision) {
                        recommendations.add(model);
                    }
                }
                break;
                
            case "reasoning":
                for (AIModel model : models) {
                    if (model.getCapabilities().supportsThinking || 
                        model.getDisplayName().contains("QwQ")) {
                        recommendations.add(model);
                    }
                }
                break;
                
            case "long_context":
                for (AIModel model : models) {
                    if (model.getCapabilities().maxContextLength >= 100000) {
                        recommendations.add(model);
                    }
                }
                break;
                
            case "multimodal":
                for (AIModel model : models) {
                    if (model.getCapabilities().supportsModality("audio") || 
                        model.getCapabilities().supportsModality("video")) {
                        recommendations.add(model);
                    }
                }
                break;
        }
        
        return recommendations;
    }

    /**
     * Compare two models
     */
    public static void compareModels(AIModel model1, AIModel model2) {
        Log.i(TAG, "\n=== Model Comparison ===");
        Log.i(TAG, model1.getDisplayName() + " vs " + model2.getDisplayName());
        
        ModelCapabilities caps1 = model1.getCapabilities();
        ModelCapabilities caps2 = model2.getCapabilities();
        
        // Compare context lengths
        Log.i(TAG, "Context Length: " + caps1.getContextLengthDisplay() + " vs " + caps2.getContextLengthDisplay());
        
        // Compare capabilities
        Log.i(TAG, "Capabilities:");
        Log.i(TAG, "  Thinking: " + caps1.supportsThinking + " vs " + caps2.supportsThinking);
        Log.i(TAG, "  Vision: " + caps1.supportsVision + " vs " + caps2.supportsVision);
        Log.i(TAG, "  Audio: " + caps1.supportsAudio + " vs " + caps2.supportsAudio);
        Log.i(TAG, "  Video: " + caps1.supportsVideo + " vs " + caps2.supportsVideo);
        Log.i(TAG, "  Document: " + caps1.supportsDocument + " vs " + caps2.supportsDocument);
        Log.i(TAG, "  Citations: " + caps1.supportsCitations + " vs " + caps2.supportsCitations);
        Log.i(TAG, "  Thinking Budget: " + caps1.supportsThinkingBudget + " vs " + caps2.supportsThinkingBudget);
        Log.i(TAG, "  MCP: " + caps1.supportsMCP + " vs " + caps2.supportsMCP);
        
        // Compare modalities
        Log.i(TAG, "Modalities: " + caps1.supportedModalities + " vs " + caps2.supportedModalities);
    }

    /**
     * Get model statistics
     */
    public static void printModelStatistics(List<AIModel> models) {
        Log.i(TAG, "\n=== Model Statistics ===");
        
        int totalModels = models.size();
        int thinkingModels = 0;
        int visionModels = 0;
        int audioModels = 0;
        int videoModels = 0;
        int documentModels = 0;
        int citationModels = 0;
        int thinkingBudgetModels = 0;
        int mcpModels = 0;
        int singleRoundModels = 0;
        
        for (AIModel model : models) {
            ModelCapabilities caps = model.getCapabilities();
            if (caps.supportsThinking) thinkingModels++;
            if (caps.supportsVision) visionModels++;
            if (caps.supportsAudio) audioModels++;
            if (caps.supportsVideo) videoModels++;
            if (caps.supportsDocument) documentModels++;
            if (caps.supportsCitations) citationModels++;
            if (caps.supportsThinkingBudget) thinkingBudgetModels++;
            if (caps.supportsMCP) mcpModels++;
            if (caps.isSingleRound) singleRoundModels++;
        }
        
        Log.i(TAG, "Total Models: " + totalModels);
        Log.i(TAG, "Thinking Support: " + thinkingModels + "/" + totalModels);
        Log.i(TAG, "Vision Support: " + visionModels + "/" + totalModels);
        Log.i(TAG, "Audio Support: " + audioModels + "/" + totalModels);
        Log.i(TAG, "Video Support: " + videoModels + "/" + totalModels);
        Log.i(TAG, "Document Support: " + documentModels + "/" + totalModels);
        Log.i(TAG, "Citations Support: " + citationModels + "/" + totalModels);
        Log.i(TAG, "Thinking Budget: " + thinkingBudgetModels + "/" + totalModels);
        Log.i(TAG, "MCP Support: " + mcpModels + "/" + totalModels);
        Log.i(TAG, "Single Round: " + singleRoundModels + "/" + totalModels);
    }
}