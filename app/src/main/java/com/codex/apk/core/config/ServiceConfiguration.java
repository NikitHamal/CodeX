package com.codex.apk.core.config;

import com.codex.apk.ai.AIProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service-wide configuration manager.
 */
public class ServiceConfiguration {
    private final Map<AIProvider, ProviderConfig> providerConfigs;
    private final Duration requestTimeout;
    private final List<AIProvider> preferredProviders;
    private final boolean enableHealthMonitoring;
    private final Duration healthCheckInterval;
    
    public ServiceConfiguration(Map<AIProvider, ProviderConfig> providerConfigs,
                              Duration requestTimeout,
                              List<AIProvider> preferredProviders,
                              boolean enableHealthMonitoring,
                              Duration healthCheckInterval) {
        this.providerConfigs = new ConcurrentHashMap<>(providerConfigs);
        this.requestTimeout = requestTimeout;
        this.preferredProviders = List.copyOf(preferredProviders);
        this.enableHealthMonitoring = enableHealthMonitoring;
        this.healthCheckInterval = healthCheckInterval;
    }
    
    public ProviderConfig getProviderConfig(AIProvider provider) {
        return providerConfigs.getOrDefault(provider, ProviderConfig.defaults(provider));
    }
    
    public Duration getRequestTimeout() { return requestTimeout; }
    public List<AIProvider> getPreferredProviders() { return preferredProviders; }
    public boolean isHealthMonitoringEnabled() { return enableHealthMonitoring; }
    public Duration getHealthCheckInterval() { return healthCheckInterval; }
    
    public void updateProviderConfig(AIProvider provider, ProviderConfig config) {
        providerConfigs.put(provider, config);
    }
    
    public static ServiceConfiguration defaults() {
        return new ServiceConfiguration(
            new ConcurrentHashMap<>(),
            Duration.ofMinutes(5),
            List.of(),
            true,
            Duration.ofMinutes(1)
        );
    }
    
    public static class Builder {
        private Map<AIProvider, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();
        private Duration requestTimeout = Duration.ofMinutes(5);
        private List<AIProvider> preferredProviders = List.of();
        private boolean enableHealthMonitoring = true;
        private Duration healthCheckInterval = Duration.ofMinutes(1);
        
        public Builder withProviderConfig(AIProvider provider, ProviderConfig config) {
            this.providerConfigs.put(provider, config);
            return this;
        }
        
        public Builder withRequestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }
        
        public Builder withPreferredProviders(List<AIProvider> providers) {
            this.preferredProviders = List.copyOf(providers);
            return this;
        }
        
        public Builder enableHealthMonitoring(boolean enable) {
            this.enableHealthMonitoring = enable;
            return this;
        }
        
        public Builder withHealthCheckInterval(Duration interval) {
            this.healthCheckInterval = interval;
            return this;
        }
        
        public ServiceConfiguration build() {
            return new ServiceConfiguration(
                providerConfigs, requestTimeout, preferredProviders,
                enableHealthMonitoring, healthCheckInterval
            );
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}