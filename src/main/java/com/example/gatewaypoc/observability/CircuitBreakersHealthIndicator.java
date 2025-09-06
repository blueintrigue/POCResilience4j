package com.example.gatewaypoc.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("downstreamCircuitBreakers")
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakersHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean anyOpen = false;
        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            Map<String, Object> item = new HashMap<>();
            item.put("state", cb.getState().name());
            item.put("metrics", Map.of(
                    "bufferedCalls", cb.getMetrics().getNumberOfBufferedCalls(),
                    "failedCalls", cb.getMetrics().getNumberOfFailedCalls(),
                    "notPermitted", cb.getMetrics().getNumberOfNotPermittedCalls()
            ));
            details.put(cb.getName(), item);
            if (cb.getState() == CircuitBreaker.State.OPEN) {
                anyOpen = true;
            }
        }
        return anyOpen ? Health.status("DEGRADED").withDetails(details).build()
                : Health.up().withDetails(details).build();
    }
}

