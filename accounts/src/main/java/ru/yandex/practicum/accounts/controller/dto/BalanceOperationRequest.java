package ru.yandex.practicum.accounts.controller.dto;

import jakarta.validation.constraints.Positive;

public record BalanceOperationRequest(

        @Positive
        long amount
) {
}