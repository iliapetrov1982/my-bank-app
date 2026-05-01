package ru.yandex.practicum.transfer.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.transfer.kafka.NotificationEvent;
import ru.yandex.practicum.transfer.kafka.NotificationKafkaProducer;
import ru.yandex.practicum.transfer.controller.dto.TransferRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;
    private final NotificationKafkaProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    public TransferResponse transfer(String fromLogin, TransferRequest request) {
        log.info("Transfer requested: from={}, to={}, amount={}",
                fromLogin, request.toLogin(), request.amount());
        try {
            accountsClient.withdraw(fromLogin, new BalanceOperationRequest(request.amount()));
            try {
                accountsClient.deposit(request.toLogin(), new BalanceOperationRequest(request.amount()));
            } catch (Exception e) {
                log.error("Deposit to {} failed during transfer, compensating refund to {}",
                        request.toLogin(), fromLogin, e);
                accountsClient.deposit(fromLogin, new BalanceOperationRequest(request.amount()));
                throw new IllegalStateException(
                        "Перевод не выполнен: ошибка зачисления на счёт %s. Средства возвращены."
                                .formatted(request.toLogin()), e);
            }
            log.debug("Transfer completed: from={}, to={}, amount={}",
                    fromLogin, request.toLogin(), request.amount());
            notificationProducer.send(new NotificationEvent(
                    fromLogin,
                    "Переведено %d руб. со счёта %s на счёт %s."
                            .formatted(request.amount(), fromLogin, request.toLogin())
            ));
            return new TransferResponse(fromLogin, request.toLogin(), request.amount());
        } catch (RuntimeException e) {
            log.warn("Transfer failed: from={}, to={}, amount={}, reason={}",
                    fromLogin, request.toLogin(), request.amount(), e.getMessage());
            meterRegistry.counter("transfer.failed",
                    "from_login", fromLogin,
                    "to_login", request.toLogin()).increment();
            throw e;
        }
    }
}