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
 * Factory for creating Gemini AI services.
 * Supports both official API (GOOGLE) and cookie-based (COOKIES) implementations.
 */
public class GeminiServiceFactory implements AIServiceFactory {
    
    private final AIProvider providerType;
    private final ProviderInfo providerInfo;
    
    public GeminiServiceFactory(AIProvider providerType) {
        if (providerType != AIProvider.GOOGLE && providerType != AIProvider.COOKIES) {
            throw new IllegalArgumentException("GeminiServiceFactory only supports GOOGLE and COOKIES providers");
        }
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
            
            switch (providerType) {
                case GOOGLE:
                    return new GeminiOfficialService(config, capabilities);
                case COOKIES:
                    return new GeminiFreeService(config, capabilities);
                default:
                    throw new ServiceCreationException("Unsupported provider type: " + providerType);
            }
        } catch (Exception e) {
            throw new ServiceCreationException("Failed to create Gemini service", e);
        }
    }
    
    @Override
    public ProviderInfo getProviderInfo() {
        return providerInfo;
    }
    
    @Override
    public ValidationResult validateConfiguration(ProviderConfig config) {
        ValidationResult.Builder result = ValidationResult.builder();
        
        switch (providerType) {
            case GOOGLE:
                // Official Gemini API requires API key
                if (!config.hasApiKey()) {
                    result.addError("API key is required for Gemini Official API");
                }
                
                // Validate base URL if provided
                if (config.hasBaseUrl()) {
                    String baseUrl = config.getBaseUrl();
                    if (!baseUrl.contains("generativelanguage.googleapis.com")) {
                        result.addWarning("Base URL should point to generativelanguage.googleapis.com for official Gemini API");
                    }
                }
                break;
                
            case COOKIES:
                // Cookie-based implementation requires PSID cookie
                String psid = config.getProviderSpecificConfig("psid", "");
                if (psid == null || psid.isEmpty()) {
                    result.addError("__Secure-1PSID cookie is required for Gemini Free (cookie-based) access");
                }
                
                // PSIDTS is optional but recommended
                String psidts = config.getProviderSpecificConfig("psidts", "");
                if (psidts == null || psidts.isEmpty()) {
                    result.addWarning("__Secure-1PSIDTS cookie is recommended for better session stability");
                }
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
            case GOOGLE:
                return new ProviderInfo(
                    provider,
                    "Google Gemini (Official)",
                    "Official Google Gemini API with full feature support including vision, tools, and advanced capabilities",
                    capabilities
                );
            case COOKIES:
                return new ProviderInfo(
                    provider,
                    "Google Gemini (Free)",
                    "Free access to Gemini using cookie-based authentication - no API key required",
                    capabilities
                );
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
    
    private ProviderCapabilities createCapabilities(AIProvider provider) {
        Set<String> supportedFormats = new HashSet<>();
        supportedFormats.add("text");
        supportedFormats.add("image");
        
        switch (provider) {
            case GOOGLE:
                return new ProviderCapabilities(
                    true,   // streaming
                    true,   // vision
                    true,   // tools
                    false,  // web search (not directly supported)
                    true,   // thinking (in some models)
                    true,   // multimodal
                    2097152, // max tokens (2M for Gemini 2.5 Pro)
                    supportedFormats
                );
            case COOKIES:
                return new ProviderCapabilities(
                    true,   // streaming
                    true,   // vision
                    false,  // tools (limited in cookie version)
                    true,   // web search (available in web interface)
                    true,   // thinking
                    true,   // multimodal
                    1048576, // max tokens (1M typical for free version)
                    supportedFormats
                );
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
    
    /**
     * Creates a factory for the Gemini Official API.
     * 
     * @return Factory for official Gemini API
     */
    public static GeminiServiceFactory createOfficial() {
        return new GeminiServiceFactory(AIProvider.GOOGLE);
    }
    
    /**
     * Creates a factory for the Gemini Free (cookie-based) implementation.
     * 
     * @return Factory for cookie-based Gemini access
     */
    public static GeminiServiceFactory createFree() {
        return new GeminiServiceFactory(AIProvider.COOKIES);
    }
    
    /**
     * Creates a factory for the specified Gemini provider type.
     * 
     * @param provider The provider type (GOOGLE or COOKIES)
     * @return Factory instance
     * @throws IllegalArgumentException if provider is not supported
     */
    public static GeminiServiceFactory create(AIProvider provider) {
        return new GeminiServiceFactory(provider);
    }
    
    /**
     * Gets all supported Gemini provider types.
     * 
     * @return Set of supported provider types
     */
    public static Set<AIProvider> getSupportedProviders() {
        Set<AIProvider> supported = new HashSet<>();
        supported.add(AIProvider.GOOGLE);
        supported.add(AIProvider.COOKIES);
        return supported;
    }
}