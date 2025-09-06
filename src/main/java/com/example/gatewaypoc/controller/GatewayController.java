package com.example.gatewaypoc.controller;

import com.example.gatewaypoc.service.ServiceAClient;
import com.example.gatewaypoc.service.ServiceBClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class GatewayController {
    private final ServiceAClient serviceAClient;
    private final ServiceBClient serviceBClient;

    public GatewayController(ServiceAClient serviceAClient, ServiceBClient serviceBClient) {
        this.serviceAClient = serviceAClient;
        this.serviceBClient = serviceBClient;
    }

    @GetMapping(value = "/composite/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<CompositeResponse> composite(@PathVariable String id) {
        Mono<String> a = serviceAClient.getData(id).onErrorResume(e -> Mono.just("serviceA-error"));
        Mono<String> b = serviceBClient.getInfo(id).onErrorResume(e -> Mono.just("serviceB-error"));
        return Mono.zip(a, b).map(t -> new CompositeResponse(id, t.getT1(), t.getT2()));
    }

    public record CompositeResponse(String id, String a, String b) {}
}

