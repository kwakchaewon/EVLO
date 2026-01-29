package com.evlo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring AI OpenAI 자동 설정이 요구하는 RestClient.Builder 빈 제공.
 * WebFlux 전용 프로젝트에서는 RestClient가 자동 등록되지 않으므로 수동 등록.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
