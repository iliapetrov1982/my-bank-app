package ru.yandex.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.cash.controller.dto.CashRequest;
import ru.yandex.practicum.cash.controller.dto.CashResponse;
import ru.yandex.practicum.cash.service.CashService;

@RestController
@RequestMapping("/api/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    /**
     * POST /api/cash/deposit
     * Пополнить счёт текущего пользователя.
     * Логин извлекается из User JWT (Authorization Code Flow).
     */
    @PostMapping("/deposit")
    public ResponseEntity<CashResponse> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CashRequest request
    ) {
        String login = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(cashService.deposit(login, request));
    }

    /**
     * POST /api/cash/withdraw
     * Снять деньги со счёта текущего пользователя.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<CashResponse> withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CashRequest request
    ) {
        String login = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(cashService.withdraw(login, request));
    }
}