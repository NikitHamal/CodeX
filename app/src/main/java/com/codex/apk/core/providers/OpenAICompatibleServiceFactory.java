package com.codex.apk.core.providers;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.ProviderInfo;
import com.codex.apk.core.model.ValidationResult;
import com.codex.apk.core.model.ProviderCapabilities;
import com.codex.apk.core.service.AIService;
import com.codex.apk.core.service.AIServiceFactory;
import com.codex.apk.ai.AIProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * Factory for creating OpenAI-compatible AI services.
 * This factory can create services for multiple providers that use the OpenAI API format.
 */
public class OpenAICompatibleServiceFactory implements AIServiceFactory {
    
    private final AIProvider providerType;
    private final ProviderInfo providerInfo;
    
    public OpenAICompatibleServiceFactory(AIProvider providerType) {
        this.providerType = providerType;
        this.providerInfo = createProviderInfo(providerType);
    }
    
    @Override
    public AIService createService(ProviderConfig config) throws ServiceCreationException {
        ValidationResult validation = validateConfiguration(config);
        if (validation.hasErrors()) {
            throw new ServiceCreationException("Configuration validation failed: " + validation.getErrors());
        }
        
        try {
            ProviderCapabilities capabilities = createCapabilities(providerType);
            return new OpenAICompatibleService(config, capabilities);
        } catch (Exception e) {
            throw new ServiceCreationException("Failed to create OpenAI-compatible service", e);
        }
    }
    
    @Override
    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }
    
    @Override
    public ValidationResult validateConfiguration(ProviderConfig config) {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (!config.hasBaseUrl()) {
            result.addError("Base URL is required for OpenAI-compatible providers");
        }
        
        // Provider-specific validations
        switch (providerType) {
            case DEEPINFRA:
                if (!config.hasApiKey()) {
                    result.addError("API key is required for DeepInfra");
                }
                break;
            case AIRFORCE:
                // API Airforce might not require API key for some endpoints
                break;
            case FREE:
                // Free endpoints typically don't require API keys
                break;
        }
        
        return result.build();
    }
    
    @Override
    public AIProvider getProviderType() {
        return providerType;
    }
    
    @Override
    public boolean requiresNetworkAccess() {
        return true;
    }
    
    private ProviderInfo createProviderInfo(AIProvider provider) {
        ProviderCapabilities capabilities = createCapabilities(provider);
        
        switch (provider) {
            case DEEPINFRA:
                return new ProviderInfo(
                    provider,
                    "DeepInfra",
                    "High-performance AI inference platform with OpenAI-compatible API",
                    capabilities
                );
            case AIRFORCE:
                return new ProviderInfo(
                    provider,
                    "API Airforce",
                    "Free OpenAI-compatible API aggregator service",
                    capabilities
                );
            case FREE:
                return new ProviderInfo(
                    provider,
                    "Free Provider",
                    "Free text generation service (Pollinations)",
                    capabilities
                );
            default:
                return new ProviderInfo(
                    provider,
                    "OpenAI Compatible",
                    "OpenAI-compatible AI service",
                    capabilities
                );
        }
    }
    
    private ProviderCapabilities createCapabilities(AIProvider provider) {
        Set<String> supportedFormats = new HashSet<>();
        supportedFormats.add("text");
        
        switch (provider) {
            case DEEPINFRA:
                return new ProviderCapabilities(
                    true,  // streaming
                    false, // vision
                    true,  // tools
                    false, // web search
                    false, // thinking
                    false, // multimodal
                    131072, // max tokens
                    supportedFormats
                );
            case AIRFORCE:
                return new ProviderCapabilities(
                    true,  // streaming
                    false, // vision
                    false, // tools
                    false, // web search
                    false, // thinking
                    false, // multimodal
                    131072, // max tokens
                    supportedFormats
                );
            case FREE:
                return new ProviderCapabilities(
                    true,  // streaming
                    false, // vision
                    false, // tools
                    false, // web search
                    false, // thinking
                    false, // multimodal
                    131072, // max tokens
                    supportedFormats
                );
            default:
                return new ProviderCapabilities(
                    true,  // streaming
                    false, // vision
                    false, // tools
                    false, // web search
                    false, // thinking
                    false, // multimodal
                    4096,  // max tokens
                    supportedFormats
                );
        }
    }
    
    /**
     * Creates a factory for the specified OpenAI-compatible provider.
     * 
     * @param provider The provider type
     * @return Factory instance
     * @throws IllegalArgumentException if provider is not OpenAI-compatible
     */
    public static OpenAICompatibleServiceFactory create(AIProvider provider) {
        switch (provider) {
            case DEEPINFRA:
            case AIRFORCE:
            case FREE:
                return new OpenAICompatibleServiceFactory(provider);
            default:
                throw new IllegalArgumentException("Provider " + provider + " is not OpenAI-compatible");
        }
    }
    
    /**
     * Gets all supported OpenAI-compatible provider types.
     * 
     * @return Set of supported provider types
     */
    public static Set<AIProvider> getSupportedProviders() {
        Set<AIProvider> supported = new HashSet<>();
        supported.add(AIProvider.DEEPINFRA);
        supported.add(AIProvider.AIRFORCE);
        supported.add(AIProvider.FREE);
        return supported;
    }
}