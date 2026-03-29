package ru.yandex.practicum.frontui.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.frontui.client.AccountsClient;
import ru.yandex.practicum.frontui.client.CashClient;
import ru.yandex.practicum.frontui.client.TransferClient;
import ru.yandex.practicum.frontui.client.dto.AccountResponse;
import ru.yandex.practicum.frontui.client.dto.AccountShortResponse;
import ru.yandex.practicum.frontui.client.dto.CashRequest;
import ru.yandex.practicum.frontui.client.dto.TransferRequest;
import ru.yandex.practicum.frontui.client.dto.UpdateAccountRequest;
import ru.yandex.practicum.frontui.controller.dto.CashAction;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FrontService {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    public AccountResponse getMyAccount(OAuth2AuthorizedClient authorizedClient) {
        return accountsClient.getMyAccount(authorizedClient);
    }

    public void updateAccount(OAuth2AuthorizedClient authorizedClient,
                              UpdateAccountRequest request) {
        accountsClient.updateMyAccount(authorizedClient, request);
    }

    public List<AccountShortResponse> getOtherAccounts(OAuth2AuthorizedClient authorizedClient) {
        return accountsClient.getOtherAccounts(authorizedClient);
    }

    public String processCash(OAuth2AuthorizedClient authorizedClient,
                              CashRequest request,
                              CashAction action) {
        if (action == CashAction.PUT) {
            cashClient.deposit(authorizedClient, request);
            return "Положено %d руб".formatted(request.amount());
        } else {
            cashClient.withdraw(authorizedClient, request);
            return "Снято %d руб".formatted(request.amount());
        }
    }

    public String transfer(OAuth2AuthorizedClient authorizedClient,
                           TransferRequest request) {
        List<AccountShortResponse> others = accountsClient.getOtherAccounts(authorizedClient);
        String toName = others.stream()
                .filter(a -> a.login().equals(request.toLogin()))
                .map(AccountShortResponse::name)
                .findFirst()
                .orElse(request.toLogin());
        transferClient.transfer(authorizedClient, request);
        return "Успешно переведено %d руб клиенту %s".formatted(request.amount(), toName);
    }
}
