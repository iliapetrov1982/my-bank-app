package ru.yandex.practicum.cash.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.cash.client.dto.AccountResponse;
import ru.yandex.practicum.cash.client.dto.BalanceOperationRequest;

@FeignClient(name = "accounts", url = "${app.accounts-url}")
public interface AccountsClient {

    @PostMapping("/api/accounts/{login}/deposit")
    AccountResponse deposit(@PathVariable String login,
                            @RequestBody BalanceOperationRequest request);

    @PostMapping("/api/accounts/{login}/withdraw")
    AccountResponse withdraw(@PathVariable String login,
                             @RequestBody BalanceOperationRequest request);
}