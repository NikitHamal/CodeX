package com.codex.apk.core.service;

import com.codex.apk.core.config.ProviderConfig;
import com.codex.apk.core.model.ProviderInfo;
import com.codex.apk.core.model.ValidationResult;
import com.codex.apk.ai.AIProvider;

/**
 * Factory interface for creating AI service instances.
 * Each provider should implement this factory to enable pluggable service creation
 * and configuration validation.
 */
public interface AIServiceFactory {
    
    /**
     * Creates a new AI service instance with the given configuration.
     * 
     * @param config Provider-specific configuration
     * @return Configured AI service instance
     * @throws ServiceCreationException if service creation fails
     */
    AIService createService(ProviderConfig config) throws ServiceCreationException;
    
    /**
     * Gets information about this provider, including display name, capabilities,
     * and supported features.
     * 
     * @return Provider information object
     */
    ProviderInfo getProviderInfo();
    
    /**
     * Validates the provided configuration before service creation.
     * This allows early validation and better error messages.
     * 
     * @param config Configuration to validate
     * @return Validation result with any errors or warnings
     */
    ValidationResult validateConfiguration(ProviderConfig config);
    
    /**
     * Gets the provider type that this factory creates services for.
     * 
     * @return Provider type enum value
     */
    AIProvider getProviderType();
    
    /**
     * Indicates whether this factory requires network access for service creation
     * (e.g., for validation or model fetching).
     * 
     * @return true if network access is required
     */
    boolean requiresNetworkAccess();
    
    /**
     * Exception thrown when service creation fails.
     */
    class ServiceCreationException extends Exception {
        public ServiceCreationException(String message) {
            super(message);
        }
        
        public ServiceCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}