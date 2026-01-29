package com.evlo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "evtx.service")
public class EvtxServiceProperties {

    private String url = "http://localhost:8081";
    private int timeoutMs = 30000;
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private int waitDuration = 1000;
    }

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureRateThreshold = 50;
        private int waitDurationInOpenState = 10000;
        private int slidingWindowSize = 10;
    }
}
