package com.example.gatewaypoc.service;

import com.example.gatewaypoc.resilience.HalfOpenAdmissionService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class ServiceBClient {
    private static final Logger log = LoggerFactory.getLogger(ServiceBClient.class);

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final HalfOpenAdmissionService halfOpenAdmissionService;

    public ServiceBClient(Map<String, WebClient> serviceWebClients,
                          CircuitBreakerRegistry circuitBreakerRegistry,
                          TimeLimiterRegistry timeLimiterRegistry,
                          HalfOpenAdmissionService halfOpenAdmissionService) {
        this.webClient = serviceWebClients.get("serviceB");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("serviceB");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("serviceB");
        this.halfOpenAdmissionService = halfOpenAdmissionService;
    }

    public Mono<String> getInfo(String id) {
        return Mono.defer(() -> {
                    halfOpenAdmissionService.checkOrThrow("serviceB");
                    return webClient.get().uri("/api/info/{id}", id)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(10));
                })
                .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorResume(throwable -> {
                    log.warn("serviceB fallback due to: {}", throwable.toString());
                    return Mono.just("serviceB-fallback:" + id);
                });
    }
}

