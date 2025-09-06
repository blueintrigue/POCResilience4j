package com.example.gatewaypoc.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebClientConfig {

    @Bean
    public Map<String, WebClient> serviceWebClients(DownstreamProperties properties, ObjectProvider<ExchangeStrategies> strategiesProvider) {
        Map<String, WebClient> clients = new HashMap<>();
        for (Map.Entry<String, DownstreamProperties.ServiceConfig> entry : properties.getServices().entrySet()) {
            String serviceName = entry.getKey();
            DownstreamProperties.ServiceConfig cfg = entry.getValue();
            HttpClient httpClient = HttpClient.create()
                    .responseTimeout(cfg.getTimeout())
                    .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.max(1000, Math.min(Integer.MAX_VALUE, cfg.getTimeout().toMillis())));

            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(cfg.getBaseUrl())
                    .clientConnector(new ReactorClientHttpConnector(httpClient));
            ExchangeStrategies strategies = strategiesProvider.getIfAvailable();
            if (strategies != null) {
                builder.exchangeStrategies(strategies);
            }
            clients.put(serviceName, builder.build());
        }
        return clients;
    }
}

