package com.codex.apk.core.config;

import com.codex.apk.ai.AIProvider;
import com.codex.apk.core.model.ValidationResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for AI providers with type-safe configuration options
 * and validation. Each provider can have its own specific configuration
 * while sharing common configuration patterns.
 */
public class ProviderConfig {
    
    private final AIProvider providerType;
    private final String baseUrl;
    private final String apiKey;
    private final TimeoutConfig timeouts;
    private final RetryPolicy retryPolicy;
    private final RateLimitConfig rateLimits;
    private final Map<String, String> customHeaders;
    private final Map<String, Object> providerSpecificConfig;
    private final boolean enabled;
    
    private ProviderConfig(Builder builder) {
        this.providerType = builder.providerType;
        this.baseUrl = builder.baseUrl;
        this.apiKey = builder.apiKey;
        this.timeouts = builder.timeouts != null ? builder.timeouts : TimeoutConfig.defaults();
        this.retryPolicy = builder.retryPolicy != null ? builder.retryPolicy : RetryPolicy.defaults();
        this.rateLimits = builder.rateLimits != null ? builder.rateLimits : RateLimitConfig.defaults();
        this.customHeaders = new HashMap<>(builder.customHeaders);
        this.providerSpecificConfig = new HashMap<>(builder.providerSpecificConfig);
        this.enabled = builder.enabled;
    }
    
    // Getters
    public AIProvider getProviderType() { return providerType; }
    public String getBaseUrl() { return baseUrl; }
    public String getApiKey() { return apiKey; }
    public TimeoutConfig getTimeouts() { return timeouts; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }
    public RateLimitConfig getRateLimits() { return rateLimits; }
    public Map<String, String> getCustomHeaders() { return new HashMap<>(customHeaders); }
    public Map<String, Object> getProviderSpecificConfig() { return new HashMap<>(providerSpecificConfig); }
    public boolean isEnabled() { return enabled; }
    
    // Convenience methods
    public boolean hasApiKey() { return apiKey != null && !apiKey.trim().isEmpty(); }
    public boolean hasBaseUrl() { return baseUrl != null && !baseUrl.trim().isEmpty(); }
    
    public <T> T getProviderSpecificConfig(String key, Class<T> type) {
        Object value = providerSpecificConfig.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return null;
    }
    
    public String getProviderSpecificConfig(String key, String defaultValue) {
        Object value = providerSpecificConfig.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }
    
    /**
     * Merges this configuration with another, with the other taking precedence
     * for non-null values.
     * 
     * @param other Configuration to merge with
     * @return New merged configuration
     */
    public ProviderConfig merge(ProviderConfig other) {
        if (other == null) return this;
        if (!this.providerType.equals(other.providerType)) {
            throw new IllegalArgumentException("Cannot merge configs for different providers");
        }
        
        return builder(this.providerType)
            .withBaseUrl(other.hasBaseUrl() ? other.baseUrl : this.baseUrl)
            .withApiKey(other.hasApiKey() ? other.apiKey : this.apiKey)
            .withTimeouts(other.timeouts != null ? other.timeouts : this.timeouts)
            .withRetryPolicy(other.retryPolicy != null ? other.retryPolicy : this.retryPolicy)
            .withRateLimits(other.rateLimits != null ? other.rateLimits : this.rateLimits)
            .withCustomHeaders(mergeHeaders(this.customHeaders, other.customHeaders))
            .withProviderSpecificConfig(mergeProviderConfig(this.providerSpecificConfig, other.providerSpecificConfig))
            .enabled(other.enabled) // Take other's enabled state
            .build();
    }
    
    /**
     * Validates this configuration for consistency and completeness.
     * 
     * @return ValidationResult indicating any issues found
     */
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (providerType == null) {
            result.addError("Provider type is required");
        }
        
        // Validate URLs if present
        if (hasBaseUrl()) {
            if (!isValidUrl(baseUrl)) {
                result.addError("Invalid base URL: " + baseUrl);
            }
        }
        
        // Provider-specific validations
        switch (providerType) {
            case GOOGLE:
                if (!hasApiKey() && !hasProviderSpecificConfig("cookies")) {
                    result.addError("Google provider requires either API key or cookies");
                }
                break;
            case ALIBABA:
                // Qwen might require specific configuration
                break;
            case DEEPINFRA:
            case AIRFORCE:
                if (!hasBaseUrl()) {
                    result.addError("OpenAI-compatible providers require base URL");
                }
                break;
        }
        
        // Validate timeouts
        ValidationResult timeoutValidation = timeouts.validate();
        result.merge(timeoutValidation);
        
        // Validate retry policy
        ValidationResult retryValidation = retryPolicy.validate();
        result.merge(retryValidation);
        
        return result.build();
    }
    
    private Map<String, String> mergeHeaders(Map<String, String> base, Map<String, String> override) {
        Map<String, String> merged = new HashMap<>(base);
        merged.putAll(override);
        return merged;
    }
    
    private Map<String, Object> mergeProviderConfig(Map<String, Object> base, Map<String, Object> override) {
        Map<String, Object> merged = new HashMap<>(base);
        merged.putAll(override);
        return merged;
    }
    
    private boolean hasProviderSpecificConfig(String key) {
        return providerSpecificConfig.containsKey(key);
    }
    
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }
    
    /**
     * Builder pattern for constructing ProviderConfig instances.
     */
    public static class Builder {
        private AIProvider providerType;
        private String baseUrl;
        private String apiKey;
        private TimeoutConfig timeouts;
        private RetryPolicy retryPolicy;
        private RateLimitConfig rateLimits;
        private Map<String, String> customHeaders = new HashMap<>();
        private Map<String, Object> providerSpecificConfig = new HashMap<>();
        private boolean enabled = true;
        
        public Builder(AIProvider providerType) {
            this.providerType = providerType;
        }
        
        public Builder withBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public Builder withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder withTimeouts(TimeoutConfig timeouts) {
            this.timeouts = timeouts;
            return this;
        }
        
        public Builder withRetryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }
        
        public Builder withRateLimits(RateLimitConfig rateLimits) {
            this.rateLimits = rateLimits;
            return this;
        }
        
        public Builder withCustomHeaders(Map<String, String> headers) {
            this.customHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
            return this;
        }
        
        public Builder addCustomHeader(String name, String value) {
            this.customHeaders.put(name, value);
            return this;
        }
        
        public Builder withProviderSpecificConfig(Map<String, Object> config) {
            this.providerSpecificConfig = config != null ? new HashMap<>(config) : new HashMap<>();
            return this;
        }
        
        public Builder addProviderSpecificConfig(String key, Object value) {
            this.providerSpecificConfig.put(key, value);
            return this;
        }
        
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        public ProviderConfig build() {
            ProviderConfig config = new ProviderConfig(this);
            ValidationResult validation = config.validate();
            if (validation.hasErrors()) {
                throw new IllegalArgumentException("Configuration validation failed: " + validation.getErrors());
            }
            return config;
        }
    }
    
    /**
     * Creates a new builder for the specified provider type.
     * 
     * @param providerType Provider type
     * @return New builder
     */
    public static Builder builder(AIProvider providerType) {
        return new Builder(providerType);
    }
    
    /**
     * Creates a default configuration for the specified provider.
     * 
     * @param providerType Provider type
     * @return Default configuration
     */
    public static ProviderConfig defaults(AIProvider providerType) {
        Builder builder = builder(providerType);
        
        // Set provider-specific defaults
        switch (providerType) {
            case GOOGLE:
                builder.withBaseUrl("https://generativelanguage.googleapis.com/v1beta/models/");
                break;
            case ALIBABA:
                builder.withBaseUrl("https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");
                break;
            case DEEPINFRA:
                builder.withBaseUrl("https://api.deepinfra.com/v1/openai/chat/completions");
                break;
            case AIRFORCE:
                builder.withBaseUrl("https://api.airforce.com/v1/chat/completions");
                break;
            case FREE:
                builder.withBaseUrl("https://text.pollinations.ai/openai");
                break;
            case COOKIES:
                builder.withBaseUrl("https://gemini.google.com/app");
                break;
        }
        
        return builder.build();
    }
}