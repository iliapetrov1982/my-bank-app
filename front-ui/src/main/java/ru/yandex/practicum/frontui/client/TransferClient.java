package ru.yandex.practicum.frontui.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.frontui.client.dto.TransferRequest;
import ru.yandex.practicum.frontui.client.dto.TransferResponse;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient restClient;

    public TransferResponse transfer(OAuth2AuthorizedClient authorizedClient,
                                     TransferRequest request) {
        return restClient.post()
                .uri("/api/transfer")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .body(request)
                .retrieve()
                .body(TransferResponse.class);
    }
}