package com.example.gatewaypoc.resilience;

import com.example.gatewaypoc.config.DownstreamProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class HalfOpenConsecutiveEnforcer {
    private static final Logger log = LoggerFactory.getLogger(HalfOpenConsecutiveEnforcer.class);

    private final CircuitBreakerRegistry registry;
    private final DownstreamProperties properties;

    private final Map<String, AtomicInteger> halfOpenConsecutiveSuccesses = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> halfOpenConsecutiveFailures = new ConcurrentHashMap<>();

    public HalfOpenConsecutiveEnforcer(CircuitBreakerRegistry registry, DownstreamProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @PostConstruct
    public void install() {
        registry.getEventPublisher().onEntryAdded(e -> attach(e.getAddedEntry()));
        for (CircuitBreaker cb : registry.getAllCircuitBreakers()) {
            attach(cb);
        }
    }

    private void attach(CircuitBreaker cb) {
        String name = cb.getName();
        cb.getEventPublisher()
                .onStateTransition(ev -> onStateTransition(cb, ev))
                .onSuccess(ev -> onSuccess(cb, ev))
                .onError(ev -> onError(cb, ev));
    }

    private void onStateTransition(CircuitBreaker cb, CircuitBreakerOnStateTransitionEvent ev) {
        if (ev.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
            resetCounters(cb.getName());
        } else if (ev.getStateTransition().getToState() == CircuitBreaker.State.CLOSED
                || ev.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
            resetCounters(cb.getName());
        }
    }

    private void onSuccess(CircuitBreaker cb, CircuitBreakerOnSuccessEvent ev) {
        if (cb.getState() == CircuitBreaker.State.HALF_OPEN) {
            String name = cb.getName();
            int target = properties.getServices()
                    .getOrDefault(name, new DownstreamProperties.ServiceConfig())
                    .getHalfOpenConsecutive().getSuccessToClose();
            int now = halfOpenConsecutiveSuccesses.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
            halfOpenConsecutiveFailures.computeIfAbsent(name, k -> new AtomicInteger()).set(0);
            if (now >= target) {
                log.warn("Enforcing HALF_OPEN -> CLOSED for {} after {} consecutive successes", name, now);
                cb.transitionToClosedState();
                resetCounters(name);
            }
        }
    }

    private void onError(CircuitBreaker cb, CircuitBreakerOnErrorEvent ev) {
        if (cb.getState() == CircuitBreaker.State.HALF_OPEN) {
            String name = cb.getName();
            int target = properties.getServices()
                    .getOrDefault(name, new DownstreamProperties.ServiceConfig())
                    .getHalfOpenConsecutive().getFailureToOpen();
            int now = halfOpenConsecutiveFailures.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
            halfOpenConsecutiveSuccesses.computeIfAbsent(name, k -> new AtomicInteger()).set(0);
            if (now >= target) {
                log.warn("Enforcing HALF_OPEN -> OPEN for {} after {} consecutive failures", name, now);
                cb.transitionToOpenState();
                resetCounters(name);
            }
        }
    }

    private void resetCounters(String name) {
        halfOpenConsecutiveSuccesses.computeIfAbsent(name, k -> new AtomicInteger()).set(0);
        halfOpenConsecutiveFailures.computeIfAbsent(name, k -> new AtomicInteger()).set(0);
    }
}

