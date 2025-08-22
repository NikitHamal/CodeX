package com.codex.apk.core.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for preventing cascading failures.
 * Monitors failure rates and automatically opens/closes the circuit based on health metrics.
 */
public class CircuitBreaker {
    
    private final String name;
    private final int failureThreshold;
    private final Duration timeout;
    private final Duration resetTimeout;
    
    private final AtomicReference<State> state;
    private final AtomicInteger failureCount;
    private final AtomicInteger successCount;
    private final AtomicLong lastFailureTime;
    private final AtomicLong lastStateChangeTime;
    
    public CircuitBreaker(String name, int failureThreshold, Duration timeout, Duration resetTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.resetTimeout = resetTimeout;
        this.state = new AtomicReference<>(State.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.successCount = new AtomicInteger(0);
        this.lastFailureTime = new AtomicLong(0);
        this.lastStateChangeTime = new AtomicLong(System.currentTimeMillis());
    }
    
    /**
     * Executes a call through the circuit breaker.
     * 
     * @param call The call to execute
     * @return The result of the call
     * @throws CircuitBreakerOpenException if the circuit is open
     * @throws Exception if the call fails
     */
    public <T> T execute(java.util.concurrent.Callable<T> call) throws Exception {
        if (!allowCall()) {
            throw new CircuitBreakerOpenException("Circuit breaker is open for: " + name);
        }
        
        try {
            T result = call.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }
    
    /**
     * Checks if a call should be allowed through the circuit breaker.
     * 
     * @return true if call is allowed
     */
    public boolean allowCall() {
        State currentState = state.get();
        
        switch (currentState) {
            case CLOSED:
                return true;
            case OPEN:
                if (shouldAttemptReset()) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        lastStateChangeTime.set(System.currentTimeMillis());
                    }
                    return state.get() == State.HALF_OPEN;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Records a successful call.
     */
    public void onSuccess() {
        successCount.incrementAndGet();
        
        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                reset();
                lastStateChangeTime.set(System.currentTimeMillis());
            }
        }
    }
    
    /**
     * Records a failed call.
     */
    public void onFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        State currentState = state.get();
        if (currentState == State.CLOSED || currentState == State.HALF_OPEN) {
            if (failureCount.get() >= failureThreshold) {
                if (state.compareAndSet(currentState, State.OPEN)) {
                    lastStateChangeTime.set(System.currentTimeMillis());
                }
            }
        }
    }
    
    /**
     * Gets the current state of the circuit breaker.
     * 
     * @return Current state
     */
    public State getState() {
        return state.get();
    }
    
    /**
     * Gets circuit breaker metrics.
     * 
     * @return Metrics object
     */
    public Metrics getMetrics() {
        return new Metrics(
            name,
            state.get(),
            failureCount.get(),
            successCount.get(),
            Instant.ofEpochMilli(lastFailureTime.get()),
            Instant.ofEpochMilli(lastStateChangeTime.get())
        );
    }
    
    /**
     * Manually resets the circuit breaker to closed state.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        successCount.set(0);
        lastStateChangeTime.set(System.currentTimeMillis());
    }
    
    /**
     * Forces the circuit breaker to open state.
     */
    public void forceOpen() {
        state.set(State.OPEN);
        lastStateChangeTime.set(System.currentTimeMillis());
    }
    
    private boolean shouldAttemptReset() {
        long timeSinceLastStateChange = System.currentTimeMillis() - lastStateChangeTime.get();
        return timeSinceLastStateChange >= resetTimeout.toMillis();
    }
    
    /**
     * Circuit breaker states.
     */
    public enum State {
        CLOSED,    // Normal operation
        OPEN,      // Circuit is open, calls are rejected
        HALF_OPEN  // Testing if service has recovered
    }
    
    /**
     * Circuit breaker metrics.
     */
    public static class Metrics {
        private final String name;
        private final State state;
        private final int failureCount;
        private final int successCount;
        private final Instant lastFailureTime;
        private final Instant lastStateChangeTime;
        
        public Metrics(String name, State state, int failureCount, int successCount,
                      Instant lastFailureTime, Instant lastStateChangeTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.lastFailureTime = lastFailureTime;
            this.lastStateChangeTime = lastStateChangeTime;
        }
        
        public String getName() { return name; }
        public State getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public Instant getLastFailureTime() { return lastFailureTime; }
        public Instant getLastStateChangeTime() { return lastStateChangeTime; }
        
        public double getFailureRate() {
            int total = failureCount + successCount;
            return total > 0 ? (double) failureCount / total : 0.0;
        }
    }
    
    /**
     * Exception thrown when circuit breaker is open.
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
    
    /**
     * Builder for circuit breaker configuration.
     */
    public static class Builder {
        private String name = "default";
        private int failureThreshold = 5;
        private Duration timeout = Duration.ofSeconds(30);
        private Duration resetTimeout = Duration.ofMinutes(1);
        
        public Builder withName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder withFailureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }
        
        public Builder withTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Builder withResetTimeout(Duration resetTimeout) {
            this.resetTimeout = resetTimeout;
            return this;
        }
        
        public CircuitBreaker build() {
            return new CircuitBreaker(name, failureThreshold, timeout, resetTimeout);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}

/**
 * Circuit breaker registry for managing multiple circuit breakers.
 */
class CircuitBreakerRegistry {
    private final java.util.concurrent.ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new java.util.concurrent.ConcurrentHashMap<>();
    
    public CircuitBreaker getOrCreate(String name, java.util.function.Supplier<CircuitBreaker> factory) {
        return circuitBreakers.computeIfAbsent(name, k -> factory.get());
    }
    
    public CircuitBreaker get(String name) {
        return circuitBreakers.get(name);
    }
    
    public java.util.Map<String, CircuitBreaker.Metrics> getAllMetrics() {
        java.util.Map<String, CircuitBreaker.Metrics> metrics = new java.util.HashMap<>();
        circuitBreakers.forEach((name, cb) -> metrics.put(name, cb.getMetrics()));
        return metrics;
    }
    
    public void resetAll() {
        circuitBreakers.values().forEach(CircuitBreaker::reset);
    }
}