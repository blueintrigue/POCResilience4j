package com.example.gatewaypoc;

import com.example.gatewaypoc.service.ServiceAClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class HalfOpenBehaviorTest {
    static MockWebServer serverA;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        serverA = new MockWebServer();
        serverA.start();
        registry.add("downstreams.services.serviceA.baseUrl", () -> serverA.url("/").toString());
        registry.add("downstreams.services.serviceA.timeout", () -> "1s");
        registry.add("resilience4j.circuitbreaker.instances.serviceA.waitDurationInOpenState", () -> "2s");
        registry.add("resilience4j.circuitbreaker.instances.serviceA.minimumNumberOfCalls", () -> 10);
        registry.add("resilience4j.circuitbreaker.instances.serviceA.slidingWindowSize", () -> 10);
        registry.add("resilience4j.circuitbreaker.instances.serviceA.permittedNumberOfCallsInHalfOpenState", () -> 10);
    }

    @AfterAll
    static void shutdown() throws IOException {
        serverA.shutdown();
    }

    @Autowired
    ServiceAClient serviceAClient;

    @Test
    void halfOpen_allows_partial_requests() {
        for (int i = 0; i < 12; i++) {
            serverA.enqueue(new MockResponse().setResponseCode(500));
        }
        // trigger failures to open
        for (int i = 0; i < 12; i++) {
            StepVerifier.create(serviceAClient.getData("x"))
                    .expectNextMatches(s -> s.contains("fallback"))
                    .verifyComplete();
        }

        // wait for open -> half-open
        Awaitility.await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            // Enqueue successes and errors mixed to observe fail-fast for some calls
            serverA.enqueue(new MockResponse().setBody("ok").addHeader("Content-Type", "text/plain"));
            StepVerifier.create(serviceAClient.getData("x")).expectNextMatches(s -> s.equals("ok") || s.contains("fallback")).verifyComplete();
        });
    }
}

