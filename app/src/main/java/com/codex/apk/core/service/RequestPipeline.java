package com.codex.apk.core.service;

import com.codex.apk.core.model.AIRequest;
import com.codex.apk.core.model.AIResponse;
import com.codex.apk.core.pipeline.RequestInterceptor;
import com.codex.apk.core.pipeline.ResponseProcessor;

import io.reactivex.rxjava3.core.Observable;

/**
 * Request pipeline for processing AI requests and responses through a chain of
 * interceptors and processors. This enables cross-cutting concerns like logging,
 * metrics, caching, rate limiting, and request/response transformation.
 */
public interface RequestPipeline {
    
    /**
     * Executes a request through the pipeline, applying all registered interceptors
     * and processors in order.
     * 
     * @param request The AI request to process
     * @param service The AI service to execute the request with
     * @return Observable stream of processed responses
     */
    Observable<AIResponse> execute(AIRequest request, AIService service);
    
    /**
     * Adds a request interceptor to the pipeline. Interceptors are executed
     * in the order they are added, before the request is sent to the service.
     * 
     * @param interceptor The interceptor to add
     * @return This pipeline instance for chaining
     */
    RequestPipeline addInterceptor(RequestInterceptor interceptor);
    
    /**
     * Adds a response processor to the pipeline. Processors are executed
     * in the order they are added, after responses are received from the service.
     * 
     * @param processor The processor to add
     * @return This pipeline instance for chaining
     */
    RequestPipeline addProcessor(ResponseProcessor processor);
    
    /**
     * Removes a request interceptor from the pipeline.
     * 
     * @param interceptor The interceptor to remove
     * @return This pipeline instance for chaining
     */
    RequestPipeline removeInterceptor(RequestInterceptor interceptor);
    
    /**
     * Removes a response processor from the pipeline.
     * 
     * @param processor The processor to remove
     * @return This pipeline instance for chaining
     */
    RequestPipeline removeProcessor(ResponseProcessor processor);
    
    /**
     * Clears all interceptors and processors from the pipeline.
     * 
     * @return This pipeline instance for chaining
     */
    RequestPipeline clear();
    
    /**
     * Gets the number of interceptors currently in the pipeline.
     * 
     * @return Number of interceptors
     */
    int getInterceptorCount();
    
    /**
     * Gets the number of processors currently in the pipeline.
     * 
     * @return Number of processors
     */
    int getProcessorCount();
}