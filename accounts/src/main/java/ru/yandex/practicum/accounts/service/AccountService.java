package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.accounts.kafka.NotificationEvent;
import ru.yandex.practicum.accounts.kafka.NotificationKafkaProducer;
import ru.yandex.practicum.accounts.controller.dto.AccountResponse;
import ru.yandex.practicum.accounts.controller.dto.AccountShortResponse;
import ru.yandex.practicum.accounts.controller.dto.BalanceOperationRequest;
import ru.yandex.practicum.accounts.controller.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dao.AccountRepository;
import ru.yandex.practicum.accounts.model.Account;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final NotificationKafkaProducer notificationProducer;

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String login) {
        Account account = findByLogin(login);
        return toAccountResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(String login, UpdateAccountRequest request) {
        Account account = findByLogin(login);
        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        Account saved = accountRepository.save(account);
        notificationProducer.send(new NotificationEvent(
                "Аккаунт %s обновлён: имя=%s, дата рождения=%s"
                        .formatted(login, request.name(), request.birthdate())
        ));
        return toAccountResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountShortResponse> getOtherAccounts(String login) {
        return accountRepository.findAllByLoginNot(login).stream()
                .map(this::toAccountShortResponse)
                .toList();
    }

    @Transactional
    public AccountResponse deposit(String login, BalanceOperationRequest request) {
        Account account = findByLogin(login);
        account.setBalance(account.getBalance() + request.amount());
        Account saved = accountRepository.save(account);
        return toAccountResponse(saved);
    }

    @Transactional
    public AccountResponse withdraw(String login, BalanceOperationRequest request) {
        Account account = findByLogin(login);
        if (account.getBalance() < request.amount()) {
            throw new IllegalStateException("Недостаточно средств на счету");
        }
        account.setBalance(account.getBalance() - request.amount());
        Account saved = accountRepository.save(account);
        return toAccountResponse(saved);
    }

    private Account findByLogin(String login) {
        return accountRepository.findById(login)
                .orElseThrow(() -> new NoSuchElementException("Аккаунт не найден: " + login));
    }

    private AccountResponse toAccountResponse(Account account) {
        return new AccountResponse(
                account.getLogin(),
                account.getName(),
                account.getBirthdate(),
                account.getBalance()
        );
    }

    private AccountShortResponse toAccountShortResponse(Account account) {
        return new AccountShortResponse(account.getLogin(), account.getName());
    }
}