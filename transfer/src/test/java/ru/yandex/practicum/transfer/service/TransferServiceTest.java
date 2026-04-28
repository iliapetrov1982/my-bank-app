package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.transfer.client.AccountsClient;
import ru.yandex.practicum.transfer.kafka.NotificationKafkaProducer;
import ru.yandex.practicum.transfer.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationKafkaProducer notificationProducer;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_callsWithdrawDepositAndSendsNotification() {
        TransferRequest request = new TransferRequest("petrov", 300L);

        TransferResponse response = transferService.transfer("ivan", request);

        assertThat(response.fromLogin(), equalTo("ivan"));
        assertThat(response.toLogin(), equalTo("petrov"));
        assertThat(response.amount(), equalTo(300L));

        verify(accountsClient).withdraw(eq("ivan"), eq(new BalanceOperationRequest(300L)));
        verify(accountsClient).deposit(eq("petrov"), eq(new BalanceOperationRequest(300L)));
        verify(notificationProducer).send(any());
    }
}