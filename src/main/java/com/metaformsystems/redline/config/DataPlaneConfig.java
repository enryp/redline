package com.metaformsystems.redline.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DataPlaneConfig {
    @Value("${dataplane.url:http://dp.localhost/app/public/api/data}")
    private String dataPlanePublicUrl;

    @Value("${dataplane.internal.url:http://dp.localhost/app/internal/api/control}")
    private String dataPlaneInternalUrl;

    @Bean
    public WebClient dataPlanePublicClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(dataPlanePublicUrl)
                .build();
    }

    @Bean
    public WebClient dataPlaneInternalClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(dataPlaneInternalUrl)
                .build();
    }
}
