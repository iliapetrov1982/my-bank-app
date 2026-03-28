package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.accounts.controller.dto.AccountResponse;
import ru.yandex.practicum.accounts.controller.dto.AccountShortResponse;
import ru.yandex.practicum.accounts.controller.dto.BalanceOperationRequest;
import ru.yandex.practicum.accounts.controller.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * GET /api/accounts/me
     * Получить данные своего аккаунта (вызывается фронтом через Gateway).
     * Логин извлекается из User JWT (Authorization Code Flow).
     */
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        String login = jwt.getSubject();
        return ResponseEntity.ok(accountService.getAccount(login));
    }

    /**
     * PUT /api/accounts/me
     * Обновить данные своего аккаунта (имя, дата рождения).
     */
    @PutMapping("/me")
    public ResponseEntity<AccountResponse> updateMyAccount(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        String login = jwt.getSubject();
        return ResponseEntity.ok(accountService.updateAccount(login, request));
    }

    /**
     * GET /api/accounts
     * Получить список других аккаунтов для блока переводов.
     * Логин текущего пользователя исключается из результата.
     */
    @GetMapping
    public ResponseEntity<List<AccountShortResponse>> getOtherAccounts(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String login = jwt.getSubject();
        return ResponseEntity.ok(accountService.getOtherAccounts(login));
    }

    /**
     * POST /api/accounts/{login}/deposit
     * Пополнить счёт. Вызывается сервисом Cash (Client Credentials JWT).
     */
    @PostMapping("/{login}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable String login,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(accountService.deposit(login, request));
    }

    /**
     * POST /api/accounts/{login}/withdraw
     * Снять деньги. Вызывается сервисами Cash и Transfer (Client Credentials JWT).
     */
    @PostMapping("/{login}/withdraw")
    public ResponseEntity<AccountResponse> withdraw(
            @PathVariable String login,
            @Valid @RequestBody BalanceOperationRequest request
    ) {
        return ResponseEntity.ok(accountService.withdraw(login, request));
    }
}