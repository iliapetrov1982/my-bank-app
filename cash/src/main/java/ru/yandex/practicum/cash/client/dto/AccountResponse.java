package ru.yandex.practicum.cash.client.dto;

import java.time.LocalDate;

public record AccountResponse(
        String login,
        String name,
        LocalDate birthdate,
        long balance
) {
}