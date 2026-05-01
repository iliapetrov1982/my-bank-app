package ru.yandex.practicum.frontui.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${app.gateway-url}")
    private String gatewayUrl;

    @Bean
    public RestClient restClient(ObservationRegistry observationRegistry) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .observationRegistry(observationRegistry)
                .build();
    }
}