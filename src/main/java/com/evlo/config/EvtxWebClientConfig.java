package com.evlo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class EvtxWebClientConfig {

    private final EvtxServiceProperties props;

    @Bean(name = "evtxWebClient")
    public WebClient evtxWebClient() {
        return WebClient.builder()
                .baseUrl(props.getUrl())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024))
                .build();
    }
}
