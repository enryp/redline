package com.metaformsystems.redline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DataPlaneConfig {
    @Value("${dataplane.url:http://dp.localhost}")
    private String dataPlaneUrl;

    @Bean
    public WebClient dataPlaneWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(dataPlaneUrl)
                .build();
    }
}
