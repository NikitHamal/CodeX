package com.codex.apk.ai;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ModelCapabilities {
    // Legacy boolean capabilities
    public final boolean supportsThinking;
    public final boolean supportsWebSearch;
    public final boolean supportsVision;
    public final boolean supportsDocument;
    public final boolean supportsVideo;
    public final boolean supportsAudio;
    public final boolean supportsCitations;
    
    // New capabilities
    public final boolean supportsThinkingBudget;
    public final boolean supportsMCP;
    public final boolean isSingleRound;
    
    // Context and generation limits
    public final int maxContextLength;
    public final int maxGenerationLength;
    public final int maxThinkingGenerationLength;
    public final int maxSummaryGenerationLength;
    
    // File limits
    public final Map<String, Integer> fileLimits;
    
    // Modality support
    public final List<String> supportedModalities;
    
    // Chat types
    public final List<String> supportedChatTypes;
    
    // MCP tools
    public final List<String> mcpTools;
    
    // Numeric ability levels (0=disabled, 1=enabled, 2=limited, 4=advanced)
    public final Map<String, Integer> abilities;

    // Legacy constructor for backward compatibility
    public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch,
                            boolean supportsVision, boolean supportsDocument,
                            boolean supportsVideo, boolean supportsAudio,
                            boolean supportsCitations, int maxContextLength,
                            int maxGenerationLength) {
        this.supportsThinking = supportsThinking;
        this.supportsWebSearch = supportsWebSearch;
        this.supportsVision = supportsVision;
        this.supportsDocument = supportsDocument;
        this.supportsVideo = supportsVideo;
        this.supportsAudio = supportsAudio;
        this.supportsCitations = supportsCitations;
        this.maxContextLength = maxContextLength;
        this.maxGenerationLength = maxGenerationLength;
        
        // Initialize new fields with defaults
        this.supportsThinkingBudget = false;
        this.supportsMCP = false;
        this.isSingleRound = false;
        this.maxThinkingGenerationLength = 0;
        this.maxSummaryGenerationLength = 0;
        this.fileLimits = new HashMap<>();
        this.supportedModalities = new ArrayList<>();
        this.supportedChatTypes = new ArrayList<>();
        this.mcpTools = new ArrayList<>();
        this.abilities = new HashMap<>();
    }

    // Enhanced constructor with all new capabilities
    public ModelCapabilities(boolean supportsThinking, boolean supportsWebSearch,
                            boolean supportsVision, boolean supportsDocument,
                            boolean supportsVideo, boolean supportsAudio,
                            boolean supportsCitations, boolean supportsThinkingBudget,
                            boolean supportsMCP, boolean isSingleRound,
                            int maxContextLength, int maxGenerationLength,
                            int maxThinkingGenerationLength, int maxSummaryGenerationLength,
                            Map<String, Integer> fileLimits, List<String> supportedModalities,
                            List<String> supportedChatTypes, List<String> mcpTools,
                            Map<String, Integer> abilities) {
        this.supportsThinking = supportsThinking;
        this.supportsWebSearch = supportsWebSearch;
        this.supportsVision = supportsVision;
        this.supportsDocument = supportsDocument;
        this.supportsVideo = supportsVideo;
        this.supportsAudio = supportsAudio;
        this.supportsCitations = supportsCitations;
        this.supportsThinkingBudget = supportsThinkingBudget;
        this.supportsMCP = supportsMCP;
        this.isSingleRound = isSingleRound;
        this.maxContextLength = maxContextLength;
        this.maxGenerationLength = maxGenerationLength;
        this.maxThinkingGenerationLength = maxThinkingGenerationLength;
        this.maxSummaryGenerationLength = maxSummaryGenerationLength;
        this.fileLimits = fileLimits != null ? fileLimits : new HashMap<>();
        this.supportedModalities = supportedModalities != null ? supportedModalities : new ArrayList<>();
        this.supportedChatTypes = supportedChatTypes != null ? supportedChatTypes : new ArrayList<>();
        this.mcpTools = mcpTools != null ? mcpTools : new ArrayList<>();
        this.abilities = abilities != null ? abilities : new HashMap<>();
    }

    // Helper method to check if a capability is supported at a specific level
    public boolean hasAbility(String capability, int minLevel) {
        Integer level = abilities.get(capability);
        return level != null && level >= minLevel;
    }

    // Helper method to check if a modality is supported
    public boolean supportsModality(String modality) {
        return supportedModalities.contains(modality);
    }

    // Helper method to check if a chat type is supported
    public boolean supportsChatType(String chatType) {
        return supportedChatTypes.contains(chatType);
    }

    // Helper method to check if an MCP tool is supported
    public boolean supportsMCPTool(String tool) {
        return mcpTools.contains(tool);
    }

    // Helper method to get file limit
    public int getFileLimit(String limitType) {
        return fileLimits.getOrDefault(limitType, 0);
    }

    // Get a summary of capabilities
    public String getCapabilitySummary() {
        StringBuilder summary = new StringBuilder();
        
        if (supportsThinking) summary.append("Thinking, ");
        if (supportsThinkingBudget) summary.append("Thinking Budget, ");
        if (supportsWebSearch) summary.append("Web Search, ");
        if (supportsVision) summary.append("Vision, ");
        if (supportsDocument) summary.append("Document, ");
        if (supportsVideo) summary.append("Video, ");
        if (supportsAudio) summary.append("Audio, ");
        if (supportsCitations) summary.append("Citations, ");
        if (supportsMCP) summary.append("MCP, ");
        
        if (summary.length() > 0) {
            summary.setLength(summary.length() - 2); // Remove last ", "
        }
        
        return summary.toString();
    }

    // Get context length in a human-readable format
    public String getContextLengthDisplay() {
        if (maxContextLength >= 1000000) {
            return String.format("%.1fM", maxContextLength / 1000000.0);
        } else if (maxContextLength >= 1000) {
            return String.format("%.1fK", maxContextLength / 1000.0);
        } else {
            return String.valueOf(maxContextLength);
        }
    }
}
