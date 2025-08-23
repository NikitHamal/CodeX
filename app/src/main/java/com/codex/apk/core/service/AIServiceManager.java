package com.codex.apk.core.service;

import com.codex.apk.core.config.ServiceConfiguration;
import com.codex.apk.core.model.AIRequest;
import com.codex.apk.core.model.AIResponse;
import com.codex.apk.core.model.ProviderInfo;
import com.codex.apk.core.model.HealthStatus;
import com.codex.apk.core.registry.ProviderRegistry;
import com.codex.apk.ai.AIProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Central service manager that orchestrates AI service operations across multiple providers.
 * Handles provider lifecycle, request routing, fallback strategies, and health monitoring.
 */
public class AIServiceManager {
    
    private final ProviderRegistry registry;
    private final RequestPipeline pipeline;
    private final ServiceConfiguration configuration;
    private final Map<AIProvider, AIService> activeServices;
    private final ProviderSelector providerSelector;
    private final HealthMonitor healthMonitor;
    private volatile AIProvider currentProvider;
    
    public AIServiceManager(ProviderRegistry registry, RequestPipeline pipeline, ServiceConfiguration configuration) {
        this.registry = registry;
        this.pipeline = pipeline;
        this.configuration = configuration;
        this.activeServices = new ConcurrentHashMap<>();
        this.providerSelector = new ProviderSelector(registry, configuration);
        this.healthMonitor = new HealthMonitor(this);
        this.currentProvider = null;
    }
    
    /**
     * Registers a provider factory with the service manager.
     * 
     * @param factory Provider factory to register
     */
    public void registerProvider(AIServiceFactory factory) {
        AIProvider type = factory.getProviderType();
        registry.register(type, factory);
        healthMonitor.startMonitoring(type);
    }
    
    /**
     * Switches to a specific provider for future requests.
     * 
     * @param provider Provider to switch to
     * @throws IllegalArgumentException if provider is not registered
     */
    public void switchProvider(AIProvider provider) {
        if (!registry.isRegistered(provider)) {
            throw new IllegalArgumentException("Provider not registered: " + provider);
        }
        this.currentProvider = provider;
    }
    
    /**
     * Executes an AI request using the appropriate provider.
     * 
     * @param request The AI request to execute
     * @param onResponse Consumer to handle streaming responses
     * @param onError Consumer to handle errors
     * @return CompletableFuture that completes when request is finished
     */
    public CompletableFuture<Void> executeRequest(AIRequest request, 
                                                 Consumer<AIResponse> onResponse, 
                                                 Consumer<Throwable> onError) {
        return CompletableFuture.supplyAsync(() -> selectProvider(request))
            .thenCompose(selection -> {
                if (selection.isFailure()) {
                    onError.accept(new ServiceException(selection.getErrorMessage()));
                    return CompletableFuture.completedFuture(null);
                }
                return pipeline.execute(request, selection.getService(), onResponse, onError);
            })
            .orTimeout(configuration.getRequestTimeout().toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(error -> {
                handleError(request, error, onResponse, onError);
                return null;
            });
    }
    
    /**
     * Gets all available providers with their information.
     * 
     * @return List of available provider information
     */
    public List<ProviderInfo> getAvailableProviders() {
        return registry.getProviderInfo();
    }

    /**
     * Exposes available provider types for internal helpers.
     */
    public java.util.Set<AIProvider> getAvailableProviderTypes() {
        return registry.getAvailableProviders();
    }
    
    /**
     * Gets the currently selected provider.
     * 
     * @return Current provider, or null if none selected
     */
    public AIProvider getCurrentProvider() {
        return currentProvider;
    }
    
    /**
     * Performs health checks on all active services.
     * 
     * @return CompletableFuture with health status map
     */
    public CompletableFuture<Map<AIProvider, HealthStatus>> performHealthChecks() {
        return healthMonitor.checkAllServices();
    }
    
    /**
     * Performs health check on a specific provider.
     * 
     * @param provider Provider to check
     * @return CompletableFuture with health status
     */
    public CompletableFuture<HealthStatus> performHealthCheck(AIProvider provider) {
        return healthMonitor.checkService(provider);
    }
    
    /**
     * Gets or creates a service instance for the specified provider.
     * 
     * @param provider Provider type
     * @return Service instance
     * @throws ServiceException if service creation fails
     */
    public AIService getService(AIProvider provider) throws ServiceException {
        return activeServices.computeIfAbsent(provider, this::createService);
    }
    
    /**
     * Shuts down all active services and cleans up resources.
     */
    public void shutdown() {
        healthMonitor.shutdown();
        activeServices.values().forEach(AIService::shutdown);
        activeServices.clear();
    }
    
    private ProviderSelectionResult selectProvider(AIRequest request) {
        // If a specific provider is set, try to use it first
        if (currentProvider != null) {
            try {
                AIService service = getService(currentProvider);
                if (service.canHandle(request)) {
                    return ProviderSelectionResult.success(service, currentProvider);
                }
            } catch (Exception e) {
                // Fall through to automatic selection
            }
        }
        
        // Use provider selector to find the best match
        return providerSelector.selectOptimal(request, registry.getAvailableProviders());
    }
    
    private AIService createService(AIProvider provider) {
        try {
            AIServiceFactory factory = registry.getFactory(provider);
            if (factory == null) {
                throw new ServiceException("No factory registered for provider: " + provider);
            }
            
            return factory.createService(configuration.getProviderConfig(provider));
        } catch (Exception e) {
            throw new ServiceException("Failed to create service for provider: " + provider, e);
        }
    }
    
    private void handleError(AIRequest request, Throwable error, 
                           Consumer<AIResponse> onResponse, Consumer<Throwable> onError) {
        // Try to find a fallback provider
        List<AIProvider> availableProviders = List.copyOf(registry.getAvailableProviders());
        for (AIProvider fallback : availableProviders) {
            if (fallback.equals(currentProvider)) continue;
            
            try {
                AIService service = getService(fallback);
                if (service.canHandle(request)) {
                    android.util.Log.w("AIServiceManager", 
                        "Falling back to provider: " + fallback + " due to error: " + error.getMessage());
                    pipeline.execute(request, service, onResponse, onError);
                    return;
                }
            } catch (Exception e) {
                // Continue to next fallback
            }
        }
        
        // No fallback available, return original error
        onError.accept(error);
    }
    
    /**
     * Provider selection result.
     */
    public static class ProviderSelectionResult {
        private final AIService service;
        private final AIProvider provider;
        private final String errorMessage;
        private final boolean success;
        
        private ProviderSelectionResult(AIService service, AIProvider provider, String errorMessage, boolean success) {
            this.service = service;
            this.provider = provider;
            this.errorMessage = errorMessage;
            this.success = success;
        }
        
        public static ProviderSelectionResult success(AIService service, AIProvider provider) {
            return new ProviderSelectionResult(service, provider, null, true);
        }
        
        public static ProviderSelectionResult failure(String errorMessage) {
            return new ProviderSelectionResult(null, null, errorMessage, false);
        }
        
        public AIService getService() { return service; }
        public AIProvider getProvider() { return provider; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return success; }
        public boolean isFailure() { return !success; }
    }
    
    /**
     * Exception thrown by service operations.
     */
    public static class ServiceException extends RuntimeException {
        public ServiceException(String message) {
            super(message);
        }
        
        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

/**
 * Provider selector for choosing optimal providers based on request requirements.
 */
class ProviderSelector {
    private final ProviderRegistry registry;
    private final ServiceConfiguration configuration;
    
    public ProviderSelector(ProviderRegistry registry, ServiceConfiguration configuration) {
        this.registry = registry;
        this.configuration = configuration;
    }
    
    public AIServiceManager.ProviderSelectionResult selectOptimal(AIRequest request, java.util.Set<AIProvider> availableProviders) {
        // Score providers based on capabilities and preferences
        AIProvider bestProvider = null;
        int bestScore = -1;
        
        for (AIProvider provider : availableProviders) {
            try {
                ProviderInfo info = registry.getProviderInfo(provider);
                if (info != null && info.getCapabilities().canHandle(request)) {
                    int score = calculateScore(provider, request, info);
                    if (score > bestScore) {
                        bestScore = score;
                        bestProvider = provider;
                    }
                }
            } catch (Exception e) {
                // Skip problematic providers
            }
        }
        
        if (bestProvider == null) {
            return AIServiceManager.ProviderSelectionResult.failure("No suitable provider found for request");
        }
        
        try {
            AIServiceFactory factory = registry.getFactory(bestProvider);
            AIService service = factory.createService(configuration.getProviderConfig(bestProvider));
            return AIServiceManager.ProviderSelectionResult.success(service, bestProvider);
        } catch (Exception e) {
            return AIServiceManager.ProviderSelectionResult.failure("Failed to create service: " + e.getMessage());
        }
    }
    
    private int calculateScore(AIProvider provider, AIRequest request, ProviderInfo info) {
        int score = 0;
        
        // Base score
        score += 100;
        
        // Prefer providers with exact capability matches
        if (request.requiresCapability("streaming") && info.getCapabilities().supportsStreaming()) {
            score += 50;
        }
        if (request.requiresCapability("vision") && info.getCapabilities().supportsVision()) {
            score += 50;
        }
        if (request.requiresCapability("tools") && info.getCapabilities().supportsTools()) {
            score += 50;
        }
        
        // Apply configuration preferences
        if (configuration.getPreferredProviders().contains(provider)) {
            score += 200;
        }
        
        return score;
    }
}

/**
 * Health monitor for tracking provider service health.
 */
class HealthMonitor {
    private final AIServiceManager serviceManager;
    private final java.util.concurrent.ScheduledExecutorService scheduler;
    private final Map<AIProvider, HealthStatus> lastHealthStatus;
    
    public HealthMonitor(AIServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.scheduler = java.util.concurrent.Executors.newScheduledThreadPool(2);
        this.lastHealthStatus = new ConcurrentHashMap<>();
    }
    
    public void startMonitoring(AIProvider provider) {
        // Start periodic health checks
        scheduler.scheduleAtFixedRate(() -> {
            checkService(provider).thenAccept(status -> {
                lastHealthStatus.put(provider, status);
                if (!status.isHealthy()) {
                    android.util.Log.w("HealthMonitor", "Provider " + provider + " unhealthy: " + status.getMessage());
                }
            });
        }, 0, 60, TimeUnit.SECONDS);
    }
    
    public CompletableFuture<HealthStatus> checkService(AIProvider provider) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AIService service = serviceManager.getService(provider);
                return service.healthCheck().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                return HealthStatus.unhealthy("Health check failed: " + e.getMessage());
            }
        });
    }
    
    public CompletableFuture<Map<AIProvider, HealthStatus>> checkAllServices() {
        Map<AIProvider, CompletableFuture<HealthStatus>> futures = new java.util.HashMap<>();
        for (AIProvider provider : serviceManager.getAvailableProviderTypes()) {
            futures.put(provider, checkService(provider));
        }
        
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<AIProvider, HealthStatus> results = new java.util.HashMap<>();
                futures.forEach((provider, future) -> {
                    try {
                        results.put(provider, future.get());
                    } catch (Exception e) {
                        results.put(provider, HealthStatus.unhealthy("Check failed: " + e.getMessage()));
                    }
                });
                return results;
            });
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}