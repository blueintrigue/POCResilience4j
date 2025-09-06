package com.example.gatewaypoc.resilience;

import com.example.gatewaypoc.config.DownstreamProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class HalfOpenAdmissionService {
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final DownstreamProperties properties;

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public HalfOpenAdmissionService(CircuitBreakerRegistry circuitBreakerRegistry, DownstreamProperties properties) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.properties = properties;
    }

    public void checkOrThrow(String serviceName) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(serviceName);
        if (cb.getState() == CircuitBreaker.State.HALF_OPEN) {
            int percent = properties.getServices()
                    .getOrDefault(serviceName, new DownstreamProperties.ServiceConfig())
                    .getHalfOpenAdmission().getAllowPercent();
            int p = Math.max(0, Math.min(100, percent));
            AtomicLong counter = counters.computeIfAbsent(serviceName, k -> new AtomicLong());
            long c = Math.floorMod(counter.getAndIncrement(), 100);
            if (c >= p) {
                throw CallNotPermittedException.createCallNotPermittedException(cb);
            }
        }
    }
}

