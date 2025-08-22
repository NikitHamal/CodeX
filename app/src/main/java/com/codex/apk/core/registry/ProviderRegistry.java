package com.codex.apk.core.registry;

import com.codex.apk.core.service.AIServiceFactory;
import com.codex.apk.core.model.ProviderInfo;
import com.codex.apk.ai.AIProvider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Registry for AI service factories that enables discovery and instantiation
 * of provider-specific services. This registry maintains a mapping of provider
 * types to their corresponding factory implementations.
 */
public class ProviderRegistry {
    
    private final Map<AIProvider, AIServiceFactory> factories = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    
    /**
     * Registers a factory for the specified provider type.
     * 
     * @param type Provider type
     * @param factory Factory implementation
     * @throws IllegalArgumentException if factory is already registered for this type
     */
    public void register(AIProvider type, AIServiceFactory factory) {
        if (type == null) {
            throw new IllegalArgumentException("Provider type cannot be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        if (!type.equals(factory.getProviderType())) {
            throw new IllegalArgumentException(
                "Factory provider type mismatch: expected " + type + ", got " + factory.getProviderType()
            );
        }
        
        synchronized (lock) {
            if (factories.containsKey(type)) {
                throw new IllegalArgumentException("Factory already registered for provider: " + type);
            }
            factories.put(type, factory);
        }
    }
    
    /**
     * Unregisters the factory for the specified provider type.
     * 
     * @param type Provider type to unregister
     * @return The previously registered factory, or null if none was registered
     */
    public AIServiceFactory unregister(AIProvider type) {
        synchronized (lock) {
            return factories.remove(type);
        }
    }
    
    /**
     * Gets the factory for the specified provider type.
     * 
     * @param type Provider type
     * @return Factory implementation, or null if not registered
     */
    public AIServiceFactory getFactory(AIProvider type) {
        return factories.get(type);
    }
    
    /**
     * Checks if a factory is registered for the specified provider type.
     * 
     * @param type Provider type
     * @return true if factory is registered
     */
    public boolean isRegistered(AIProvider type) {
        return factories.containsKey(type);
    }
    
    /**
     * Gets all registered provider types.
     * 
     * @return Set of registered provider types
     */
    public Set<AIProvider> getAvailableProviders() {
        return Collections.unmodifiableSet(factories.keySet());
    }
    
    /**
     * Gets information about all registered providers.
     * 
     * @return List of provider information
     */
    public List<ProviderInfo> getProviderInfo() {
        List<ProviderInfo> infos = new ArrayList<>();
        for (AIServiceFactory factory : factories.values()) {
            infos.add(factory.getProviderInfo());
        }
        return infos;
    }
    
    /**
     * Gets information about a specific provider.
     * 
     * @param type Provider type
     * @return Provider information, or null if not registered
     */
    public ProviderInfo getProviderInfo(AIProvider type) {
        AIServiceFactory factory = factories.get(type);
        return factory != null ? factory.getProviderInfo() : null;
    }
    
    /**
     * Gets the number of registered providers.
     * 
     * @return Number of registered providers
     */
    public int size() {
        return factories.size();
    }
    
    /**
     * Checks if the registry is empty.
     * 
     * @return true if no providers are registered
     */
    public boolean isEmpty() {
        return factories.isEmpty();
    }
    
    /**
     * Clears all registered factories.
     */
    public void clear() {
        synchronized (lock) {
            factories.clear();
        }
    }
    
    /**
     * Gets factories that require network access for service creation.
     * 
     * @return List of factories requiring network access
     */
    public List<AIServiceFactory> getNetworkRequiredFactories() {
        List<AIServiceFactory> networkFactories = new ArrayList<>();
        for (AIServiceFactory factory : factories.values()) {
            if (factory.requiresNetworkAccess()) {
                networkFactories.add(factory);
            }
        }
        return networkFactories;
    }
    
    /**
     * Gets factories that can create services without network access.
     * 
     * @return List of factories not requiring network access
     */
    public List<AIServiceFactory> getOfflineFactories() {
        List<AIServiceFactory> offlineFactories = new ArrayList<>();
        for (AIServiceFactory factory : factories.values()) {
            if (!factory.requiresNetworkAccess()) {
                offlineFactories.add(factory);
            }
        }
        return offlineFactories;
    }
    
    @Override
    public String toString() {
        return "ProviderRegistry{" +
                "providers=" + factories.keySet() +
                '}';
    }
}