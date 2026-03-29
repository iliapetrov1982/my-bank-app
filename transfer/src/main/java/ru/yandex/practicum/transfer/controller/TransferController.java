package ru.yandex.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.transfer.controller.dto.TransferRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferResponse;
import ru.yandex.practicum.transfer.service.TransferService;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * POST /api/transfer
     * Перевести деньги со счёта текущего пользователя на счёт другого.
     * Логин отправителя извлекается из User JWT (Authorization Code Flow).
     */
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request
    ) {
        String fromLogin = jwt.getClaimAsString("preferred_username");
        return ResponseEntity.ok(transferService.transfer(fromLogin, request));
    }
}