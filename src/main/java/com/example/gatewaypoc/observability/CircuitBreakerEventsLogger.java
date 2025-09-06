package com.example.gatewaypoc.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.circuitbreaker.event.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CircuitBreakerEventsLogger {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventsLogger.class);
    private final CircuitBreakerRegistry registry;

    public CircuitBreakerEventsLogger(CircuitBreakerRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void registerListeners() {
        for (CircuitBreaker cb : registry.getAllCircuitBreakers()) {
            cb.getEventPublisher()
                    .onStateTransition(e -> log.warn("CB {} transitioned {} -> {}", cb.getName(), e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                    .onCallNotPermitted(e -> log.warn("CB {} call not permitted", cb.getName()))
                    .onError(e -> log.warn("CB {} error: {}", cb.getName(), e.getThrowable().toString()))
                    .onSuccess(e -> log.debug("CB {} success", cb.getName()));
        }
        registry.getEventPublisher().onEntryAdded(event -> {
            CircuitBreaker added = event.getAddedEntry();
            added.getEventPublisher()
                    .onStateTransition(e -> log.warn("CB {} transitioned {} -> {}", added.getName(), e.getStateTransition().getFromState(), e.getStateTransition().getToState()))
                    .onCallNotPermitted(e -> log.warn("CB {} call not permitted", added.getName()))
                    .onError(e -> log.warn("CB {} error: {}", added.getName(), e.getThrowable().toString()))
                    .onSuccess(e -> log.debug("CB {} success", added.getName()));
        });
    }
}

