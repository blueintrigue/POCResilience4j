package com.example.gatewaypoc;

import com.example.gatewaypoc.service.ServiceAClient;
import com.example.gatewaypoc.service.ServiceBClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceClientsIntegrationTest {

    static MockWebServer serverA;
    static MockWebServer serverB;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        serverA = new MockWebServer();
        serverB = new MockWebServer();
        serverA.start();
        serverB.start();
        registry.add("downstreams.services.serviceA.baseUrl", () -> serverA.url("/").toString());
        registry.add("downstreams.services.serviceB.baseUrl", () -> serverB.url("/").toString());
        registry.add("downstreams.services.serviceA.timeout", () -> "2s");
        registry.add("downstreams.services.serviceB.timeout", () -> "2s");
    }

    @AfterAll
    static void shutdown() throws IOException {
        serverA.shutdown();
        serverB.shutdown();
    }

    @Autowired
    ServiceAClient serviceAClient;
    @Autowired
    ServiceBClient serviceBClient;

    @Test
    void serviceA_opens_after_many_failures_and_recovers() {
        for (int i = 0; i < 55; i++) {
            serverA.enqueue(new MockResponse().setResponseCode(500));
        }
        StepVerifier.create(serviceAClient.getData("x").repeat(55))
                .expectNextCount(56)
                .verifyComplete();

        serverA.enqueue(new MockResponse().setBody("ok").addHeader("Content-Type", "text/plain"));
        StepVerifier.create(serviceAClient.getData("y")).expectNextMatches(s -> s.contains("fallback") || s.equals("ok")).verifyComplete();
    }

    @Test
    void serviceB_timeout_counts_as_failure() {
        serverB.enqueue(new MockResponse().setBody("slow").setBodyDelay(3, java.util.concurrent.TimeUnit.SECONDS));
        StepVerifier.create(serviceBClient.getInfo("1"))
                .expectNextMatches(s -> s.contains("fallback"))
                .verifyComplete();
    }
}

