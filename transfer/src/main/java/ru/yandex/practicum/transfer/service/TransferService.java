package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.client.NotificationsClient;
import ru.yandex.practicum.transfer.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.transfer.client.dto.NotificationRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferResponse;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    public TransferResponse transfer(String fromLogin, TransferRequest request) {
        accountsClient.withdraw(fromLogin, new BalanceOperationRequest(request.amount()));
        try {
            accountsClient.deposit(request.toLogin(), new BalanceOperationRequest(request.amount()));
        } catch (Exception e) {
            // компенсирующий вызов — возвращаем деньги отправителю
            accountsClient.deposit(fromLogin, new BalanceOperationRequest(request.amount()));
            throw new IllegalStateException(
                    "Перевод не выполнен: ошибка зачисления на счёт %s. Средства возвращены."
                            .formatted(request.toLogin()), e);
        }
        notificationsClient.send(new NotificationRequest(
                "Переведено %d руб. со счёта %s на счёт %s."
                        .formatted(request.amount(), fromLogin, request.toLogin())
        ));
        return new TransferResponse(fromLogin, request.toLogin(), request.amount());
    }
}