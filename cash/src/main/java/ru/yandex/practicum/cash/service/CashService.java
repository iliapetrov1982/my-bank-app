package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.dto.AccountResponse;
import ru.yandex.practicum.cash.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.cash.kafka.NotificationEvent;
import ru.yandex.practicum.cash.kafka.NotificationKafkaProducer;
import ru.yandex.practicum.cash.controller.dto.CashRequest;
import ru.yandex.practicum.cash.controller.dto.CashResponse;

@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;
    private final NotificationKafkaProducer notificationProducer;

    public CashResponse deposit(String login, CashRequest request) {
        AccountResponse account = accountsClient.deposit(login,
                new BalanceOperationRequest(request.amount()));
        notificationProducer.send(new NotificationEvent(
                "Счёт %s пополнен на %d руб. Текущий баланс: %d руб."
                        .formatted(login, request.amount(), account.balance())
        ));
        return new CashResponse(account.login(), account.balance());
    }

    public CashResponse withdraw(String login, CashRequest request) {
        AccountResponse account = accountsClient.withdraw(login,
                new BalanceOperationRequest(request.amount()));
        notificationProducer.send(new NotificationEvent(
                "Со счёта %s снято %d руб. Текущий баланс: %d руб."
                        .formatted(login, request.amount(), account.balance())
        ));
        return new CashResponse(account.login(), account.balance());
    }
}