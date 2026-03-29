package ru.yandex.practicum.cash.controller.dto;

import jakarta.validation.constraints.Positive;

public record CashRequest(@Positive long amount) {
}