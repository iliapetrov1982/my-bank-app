package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.controller.dto.BalanceOperationRequest;
import ru.yandex.practicum.accounts.controller.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dao.AccountRepository;
import ru.yandex.practicum.accounts.model.Account;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class AccountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private NotificationsClient notificationsClient;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.saveAll(List.of(
                Account.builder()
                        .login("ivan")
                        .name("Иванов Иван")
                        .birthdate(LocalDate.of(1990, 1, 1))
                        .balance(1000L)
                        .build(),
                Account.builder()
                        .login("petrov")
                        .name("Петров Пётр")
                        .birthdate(LocalDate.of(1985, 6, 15))
                        .balance(500L)
                        .build()
        ));
    }

    @Test
    void getAccount_existingLogin_returnsAccount() {
        var response = accountService.getAccount("ivan");

        assertThat(response, notNullValue());
        assertThat(response.login(), equalTo("ivan"));
        assertThat(response.balance(), equalTo(1000L));
    }

    @Test
    void getOtherAccounts_excludesCurrentUser() {
        var others = accountService.getOtherAccounts("ivan");

        assertThat(others, hasSize(1));
        assertThat(others.get(0).login(), equalTo("petrov"));
    }

    @Test
    void deposit_increasesBalance() {
        var response = accountService.deposit("ivan", new BalanceOperationRequest(500L));

        assertThat(response.balance(), equalTo(1500L));

        long persisted = accountRepository.findById("ivan").orElseThrow().getBalance();
        assertThat(persisted, equalTo(1500L));
    }

    @Test
    void withdraw_decreasesBalance() {
        var response = accountService.withdraw("ivan", new BalanceOperationRequest(300L));

        assertThat(response.balance(), equalTo(700L));

        long persisted = accountRepository.findById("ivan").orElseThrow().getBalance();
        assertThat(persisted, equalTo(700L));
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        assertThrows(IllegalStateException.class,
                () -> accountService.withdraw("ivan", new BalanceOperationRequest(9999L)));

        // баланс не изменился
        long persisted = accountRepository.findById("ivan").orElseThrow().getBalance();
        assertThat(persisted, equalTo(1000L));
    }

    @Test
    void updateAccount_persistsChanges() {
        var request = new UpdateAccountRequest("Новое Имя", LocalDate.of(2000, 5, 20));
        var response = accountService.updateAccount("ivan", request);

        assertThat(response.name(), equalTo("Новое Имя"));

        var persisted = accountRepository.findById("ivan").orElseThrow();
        assertThat(persisted.getName(), equalTo("Новое Имя"));
        assertThat(persisted.getBirthdate(), equalTo(LocalDate.of(2000, 5, 20)));
    }
}