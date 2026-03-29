package ru.yandex.practicum.frontui.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.frontui.client.dto.CashRequest;
import ru.yandex.practicum.frontui.client.dto.CashResponse;

@Component
@RequiredArgsConstructor
public class CashClient {

    private final RestClient restClient;

    public CashResponse deposit(OAuth2AuthorizedClient authorizedClient, CashRequest request) {
        return restClient.post()
                .uri("/api/cash/deposit")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .body(request)
                .retrieve()
                .body(CashResponse.class);
    }

    public CashResponse withdraw(OAuth2AuthorizedClient authorizedClient, CashRequest request) {
        return restClient.post()
                .uri("/api/cash/withdraw")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .body(request)
                .retrieve()
                .body(CashResponse.class);
    }
}