package ru.yandex.practicum.frontui.client;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.frontui.client.dto.AccountResponse;
import ru.yandex.practicum.frontui.client.dto.AccountShortResponse;
import ru.yandex.practicum.frontui.client.dto.UpdateAccountRequest;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient restClient;

    public AccountResponse getMyAccount(OAuth2AuthorizedClient authorizedClient) {
        return restClient.get()
                .uri("/api/accounts/me")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .retrieve()
                .body(AccountResponse.class);
    }

    public AccountResponse updateMyAccount(OAuth2AuthorizedClient authorizedClient,
                                           UpdateAccountRequest request) {
        return restClient.put()
                .uri("/api/accounts/me")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }

    public List<AccountShortResponse> getOtherAccounts(OAuth2AuthorizedClient authorizedClient) {
        return restClient.get()
                .uri("/api/accounts")
                .headers(h -> h.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }
}