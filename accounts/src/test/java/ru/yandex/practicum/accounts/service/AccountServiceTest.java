package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.controller.dto.AccountResponse;
import ru.yandex.practicum.accounts.controller.dto.BalanceOperationRequest;
import ru.yandex.practicum.accounts.controller.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dao.AccountRepository;
import ru.yandex.practicum.accounts.model.Account;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private AccountService accountService;

    private Account ivan;
    private Account petrov;

    @BeforeEach
    void setUp() {
        ivan = Account.builder()
                .login("ivan")
                .name("Иванов Иван")
                .birthdate(LocalDate.of(1990, 1, 25))
                .balance(1000L)
                .build();

        petrov = Account.builder()
                .login("petrov")
                .name("Петров Пётр")
                .birthdate(LocalDate.of(1985, 5, 10))
                .balance(500L)
                .build();
    }

    // --- getAccount ---

    @Test
    void getAccount_existingLogin_returnsAccountResponse() {
        when(accountRepository.findById("ivan")).thenReturn(Optional.of(ivan));

        AccountResponse response = accountService.getAccount("ivan");

        assertThat(response.login(), equalTo("ivan"));
        assertThat(response.name(), equalTo("Иванов Иван"));
        assertThat(response.balance(), equalTo(1000L));
    }

    @Test
    void getAccount_unknownLogin_throwsNoSuchElementException() {
        when(accountRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> accountService.getAccount("unknown"));
    }

    // --- updateAccount ---

    @Test
    void updateAccount_validRequest_updatesAndSendsNotification() {
        when(accountRepository.findById("ivan")).thenReturn(Optional.of(ivan));
        when(accountRepository.save(any())).thenReturn(ivan);

        UpdateAccountRequest request = new UpdateAccountRequest("Иван Новый", LocalDate.of(1991, 3, 15));
        AccountResponse response = accountService.updateAccount("ivan", request);

        assertThat(response.login(), equalTo("ivan"));
        verify(notificationsClient).send(any());
    }

    // --- getOtherAccounts ---

    @Test
    void getOtherAccounts_returnsAllExceptCurrentUser() {
        when(accountRepository.findAllByLoginNot("ivan")).thenReturn(List.of(petrov));

        var result = accountService.getOtherAccounts("ivan");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).login(), equalTo("petrov"));
    }

    // --- deposit ---

    @Test
    void deposit_validAmount_increasesBalance() {
        when(accountRepository.findById("ivan")).thenReturn(Optional.of(ivan));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.deposit("ivan", new BalanceOperationRequest(200L));

        assertThat(response.balance(), equalTo(1200L));
        verify(notificationsClient).send(any());
    }

    // --- withdraw ---

    @Test
    void withdraw_sufficientBalance_decreasesBalance() {
        when(accountRepository.findById("ivan")).thenReturn(Optional.of(ivan));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.withdraw("ivan", new BalanceOperationRequest(300L));

        assertThat(response.balance(), equalTo(700L));
        verify(notificationsClient).send(any());
    }

    @Test
    void withdraw_insufficientBalance_throwsIllegalStateException() {
        when(accountRepository.findById("ivan")).thenReturn(Optional.of(ivan));

        assertThrows(IllegalStateException.class,
                () -> accountService.withdraw("ivan", new BalanceOperationRequest(9999L)));

        verify(accountRepository, never()).save(any());
        verify(notificationsClient, never()).send(any());
    }
}