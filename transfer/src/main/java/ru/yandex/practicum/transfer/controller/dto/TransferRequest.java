package ru.yandex.practicum.transfer.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank String toLogin,
        @Positive long amount
) {
}