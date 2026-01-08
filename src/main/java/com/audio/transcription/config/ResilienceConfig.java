package com.audio.transcription.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for resilience patterns: Circuit Breaker and Retry.
 */
@Configuration
public class ResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Configure Circuit Breaker for Gemini API calls.
     */
    @Bean
    public CircuitBreaker geminiCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .recordExceptions(Exception.class)
                .build();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("gemini-api", config);
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> logger.debug("Circuit Breaker Success: {}", event))
                .onError(event -> logger.warn("Circuit Breaker Error: {}", event))
                .onStateTransition(event -> logger.info("Circuit Breaker State Change: {}", event));

        return circuitBreaker;
    }

    /**
     * Configure Retry policy for Gemini API calls.
     */
    @Bean
    public Retry geminiRetry(RetryRegistry retryRegistry, GeminiProperties geminiProperties) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(geminiProperties.getMaxRetries())
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                        geminiProperties.getRetryDelayMs(),
                        2.0
                ))
                .retryExceptions(Exception.class)
                .build();

        return retryRegistry.retry("gemini-api", retryConfig);
    }

    /**
     * Register event consumers for monitoring.
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<CircuitBreaker>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                logger.debug("CircuitBreaker registered: {}", event.getAddedEntry().getName());
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
                logger.debug("CircuitBreaker removed: {}", event.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(io.github.resilience4j.core.registry.EntryReplacedEvent<CircuitBreaker> event) {
                logger.debug("CircuitBreaker replaced: {}", event.getNewEntry().getName());
            }
        };
    }
}
