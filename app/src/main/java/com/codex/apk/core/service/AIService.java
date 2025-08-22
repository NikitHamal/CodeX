package com.codex.apk.core.service;

import com.codex.apk.core.model.AIRequest;
import com.codex.apk.core.model.AIResponse;
import com.codex.apk.core.model.ProviderCapabilities;
import com.codex.apk.core.model.HealthStatus;
import com.codex.apk.ai.AIModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Observable;

/**
 * Universal AI Service interface that all provider implementations must implement.
 * This interface provides a unified contract for AI interactions across different providers,
 * supporting both streaming and non-streaming responses, capability negotiation,
 * and health monitoring.
 */
public interface AIService {
    
    /**
     * Sends a message to the AI service and returns an observable stream of responses.
     * Supports both streaming and non-streaming modes based on the request configuration.
     * 
     * @param request The AI request containing message, parameters, and context
     * @return Observable stream of AI responses (single response for non-streaming, multiple for streaming)
     */
    Observable<AIResponse> sendMessage(AIRequest request);
    
    /**
     * Gets the capabilities of this AI service, including supported features,
     * token limits, and other provider-specific characteristics.
     * 
     * @return Provider capabilities object
     */
    ProviderCapabilities getCapabilities();
    
    /**
     * Retrieves the list of available models from this provider.
     * This may involve network calls and should be cached appropriately.
     * 
     * @return CompletableFuture containing list of available models
     */
    CompletableFuture<List<AIModel>> getModels();
    
    /**
     * Performs a health check on the service to verify connectivity and availability.
     * 
     * @return CompletableFuture containing health status
     */
    CompletableFuture<HealthStatus> healthCheck();
    
    /**
     * Validates whether this service can handle the given request based on its capabilities.
     * 
     * @param request The request to validate
     * @return true if the service can handle the request, false otherwise
     */
    boolean canHandle(AIRequest request);
    
    /**
     * Optimizes the request for this specific provider, potentially modifying
     * parameters, message format, or other provider-specific optimizations.
     * 
     * @param request The original request
     * @return Optimized request for this provider
     */
    AIRequest optimizeRequest(AIRequest request);
    
    /**
     * Gets the provider type identifier for this service.
     * 
     * @return Provider type enum value
     */
    com.codex.apk.ai.AIProvider getProviderType();
    
    /**
     * Gracefully shuts down the service, cleaning up resources like HTTP clients,
     * thread pools, and other resources.
     */
    void shutdown();
}