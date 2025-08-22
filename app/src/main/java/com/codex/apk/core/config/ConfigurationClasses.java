package com.codex.apk.core.config;

import com.codex.apk.core.model.ValidationResult;
import com.codex.apk.ai.AIProvider;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timeout configuration for HTTP requests.
 */
public class TimeoutConfig {
    private final Duration connectionTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;
    private final Duration totalTimeout;
    
    public TimeoutConfig(Duration connectionTimeout, Duration readTimeout, Duration writeTimeout, Duration totalTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.totalTimeout = totalTimeout;
    }
    
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public Duration getWriteTimeout() { return writeTimeout; }
    public Duration getTotalTimeout() { return totalTimeout; }
    
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (connectionTimeout.isNegative()) {
            result.addError("Connection timeout cannot be negative");
        }
        if (readTimeout.isNegative()) {
            result.addError("Read timeout cannot be negative");
        }
        if (writeTimeout.isNegative()) {
            result.addError("Write timeout cannot be negative");
        }
        if (totalTimeout.isNegative()) {
            result.addError("Total timeout cannot be negative");
        }
        
        return result.build();
    }
    
    public static TimeoutConfig defaults() {
        return new TimeoutConfig(
            Duration.ofSeconds(30),  // connection
            Duration.ofSeconds(60),  // read
            Duration.ofSeconds(30),  // write  
            Duration.ofMinutes(5)    // total
        );
    }
}

/**
 * Retry policy configuration.
 */
public class RetryPolicy {
    private final int maxRetries;
    private final Duration backoffDelay;
    private final double backoffMultiplier;
    private final Duration maxBackoffDelay;
    
    public RetryPolicy(int maxRetries, Duration backoffDelay, double backoffMultiplier, Duration maxBackoffDelay) {
        this.maxRetries = maxRetries;
        this.backoffDelay = backoffDelay;
        this.backoffMultiplier = backoffMultiplier;
        this.maxBackoffDelay = maxBackoffDelay;
    }
    
    public int getMaxRetries() { return maxRetries; }
    public Duration getBackoffDelay() { return backoffDelay; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public Duration getMaxBackoffDelay() { return maxBackoffDelay; }
    
    public ValidationResult validate() {
        ValidationResult.Builder result = ValidationResult.builder();
        
        if (maxRetries < 0) {
            result.addError("Max retries cannot be negative");
        }
        if (maxRetries > 10) {
            result.addWarning("Max retries is very high: " + maxRetries);
        }
        if (backoffDelay.isNegative()) {
            result.addError("Backoff delay cannot be negative");
        }
        if (backoffMultiplier <= 0) {
            result.addError("Backoff multiplier must be positive");
        }
        
        return result.build();
    }
    
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30));
    }
    
    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ZERO, 1.0, Duration.ZERO);
    }
}

/**
 * Rate limiting configuration.
 */
public class RateLimitConfig {
    private final int requestsPerSecond;
    private final int burstCapacity;
    private final int queueSize;
    
    public RateLimitConfig(int requestsPerSecond, int burstCapacity, int queueSize) {
        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
        this.queueSize = queueSize;
    }
    
    public int getRequestsPerSecond() { return requestsPerSecond; }
    public int getBurstCapacity() { return burstCapacity; }
    public int getQueueSize() { return queueSize; }
    
    public static RateLimitConfig defaults() {
        return new RateLimitConfig(10, 20, 100);
    }
    
    public static RateLimitConfig unlimited() {
        return new RateLimitConfig(Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }
}

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