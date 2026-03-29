package ru.yandex.practicum.accounts.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

@Configuration
public class OAuthFeignConfig {

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public OAuthFeignConfig(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Bean
    public RequestInterceptor oauthRequestInterceptor() {
        return requestTemplate -> {
            var authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("keycloak")
                    .principal("accounts-service")
                    .build();
            var client = authorizedClientManager.authorize(authorizeRequest);
            if (client != null) {
                requestTemplate.header("Authorization",
                        "Bearer " + client.getAccessToken().getTokenValue());
            }
        };
    }
}