package com.example.gatewaypoc.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "downstreams")
@Validated
public class DownstreamProperties {

    private Map<String, ServiceConfig> services = new HashMap<>();

    public Map<String, ServiceConfig> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceConfig> services) {
        this.services = services;
    }

    public static class ServiceConfig {
        @NotBlank
        private String baseUrl;

        @NotNull
        private Duration timeout = Duration.ofSeconds(2);

        private HalfOpenAdmission halfOpenAdmission = new HalfOpenAdmission();
        private HalfOpenConsecutive halfOpenConsecutive = new HalfOpenConsecutive();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public HalfOpenAdmission getHalfOpenAdmission() {
            return halfOpenAdmission;
        }

        public void setHalfOpenAdmission(HalfOpenAdmission halfOpenAdmission) {
            this.halfOpenAdmission = halfOpenAdmission;
        }

        public HalfOpenConsecutive getHalfOpenConsecutive() {
            return halfOpenConsecutive;
        }

        public void setHalfOpenConsecutive(HalfOpenConsecutive halfOpenConsecutive) {
            this.halfOpenConsecutive = halfOpenConsecutive;
        }
    }

    public static class HalfOpenAdmission {
        @Min(0)
        @Max(100)
        private int allowPercent = 50; // allow 50% of requests while half-open

        public int getAllowPercent() {
            return allowPercent;
        }

        public void setAllowPercent(int allowPercent) {
            this.allowPercent = allowPercent;
        }
    }

    public static class HalfOpenConsecutive {
        @Min(1)
        private int successToClose = 50;
        @Min(1)
        private int failureToOpen = 50;

        public int getSuccessToClose() {
            return successToClose;
        }

        public void setSuccessToClose(int successToClose) {
            this.successToClose = successToClose;
        }

        public int getFailureToOpen() {
            return failureToOpen;
        }

        public void setFailureToOpen(int failureToOpen) {
            this.failureToOpen = failureToOpen;
        }
    }
}

