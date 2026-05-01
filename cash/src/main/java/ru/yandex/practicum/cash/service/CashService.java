package ru.yandex.practicum.cash.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.dto.AccountResponse;
import ru.yandex.practicum.cash.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.cash.kafka.NotificationEvent;
import ru.yandex.practicum.cash.kafka.NotificationKafkaProducer;
import ru.yandex.practicum.cash.controller.dto.CashRequest;
import ru.yandex.practicum.cash.controller.dto.CashResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountsClient accountsClient;
    private final NotificationKafkaProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    public CashResponse deposit(String login, CashRequest request) {
        log.info("Deposit requested: login={}, amount={}", login, request.amount());
        AccountResponse account = accountsClient.deposit(login,
                new BalanceOperationRequest(request.amount()));
        log.debug("Deposit applied: login={}, newBalance={}", login, account.balance());
        notificationProducer.send(new NotificationEvent(
                login,
                "Счёт %s пополнен на %d руб. Текущий баланс: %d руб."
                        .formatted(login, request.amount(), account.balance())
        ));
        return new CashResponse(account.login(), account.balance());
    }

    public CashResponse withdraw(String login, CashRequest request) {
        log.info("Withdraw requested: login={}, amount={}", login, request.amount());
        try {
            AccountResponse account = accountsClient.withdraw(login,
                    new BalanceOperationRequest(request.amount()));
            log.debug("Withdraw applied: login={}, newBalance={}", login, account.balance());
            notificationProducer.send(new NotificationEvent(
                    login,
                    "Со счёта %s снято %d руб. Текущий баланс: %d руб."
                            .formatted(login, request.amount(), account.balance())
            ));
            return new CashResponse(account.login(), account.balance());
        } catch (RuntimeException e) {
            log.warn("Withdraw failed: login={}, amount={}, reason={}", login, request.amount(), e.getMessage());
            meterRegistry.counter("cash.withdraw.failed", "login", login).increment();
            throw e;
        }
    }
}