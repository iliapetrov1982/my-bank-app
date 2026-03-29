package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.cash.client.AccountsClient;
import ru.yandex.practicum.cash.client.NotificationsClient;
import ru.yandex.practicum.cash.client.dto.AccountResponse;
import ru.yandex.practicum.cash.client.dto.BalanceOperationRequest;
import ru.yandex.practicum.cash.controller.dto.CashRequest;
import ru.yandex.practicum.cash.controller.dto.CashResponse;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private CashService cashService;

    private static final AccountResponse IVAN_ACCOUNT = new AccountResponse(
            "ivan", "Иванов Иван", LocalDate.of(1990, 1, 25), 1200L
    );

    @Test
    void deposit_callsAccountsClientAndSendsNotification() {
        when(accountsClient.deposit(eq("ivan"), any())).thenReturn(IVAN_ACCOUNT);

        CashResponse response = cashService.deposit("ivan", new CashRequest(200L));

        assertThat(response.login(), equalTo("ivan"));
        assertThat(response.balance(), equalTo(1200L));
        verify(accountsClient).deposit(eq("ivan"), eq(new BalanceOperationRequest(200L)));
        verify(notificationsClient).send(any());
    }

    @Test
    void withdraw_callsAccountsClientAndSendsNotification() {
        AccountResponse afterWithdraw = new AccountResponse(
                "ivan", "Иванов Иван", LocalDate.of(1990, 1, 25), 800L
        );
        when(accountsClient.withdraw(eq("ivan"), any())).thenReturn(afterWithdraw);

        CashResponse response = cashService.withdraw("ivan", new CashRequest(200L));

        assertThat(response.login(), equalTo("ivan"));
        assertThat(response.balance(), equalTo(800L));
        verify(accountsClient).withdraw(eq("ivan"), eq(new BalanceOperationRequest(200L)));
        verify(notificationsClient).send(any());
    }
}