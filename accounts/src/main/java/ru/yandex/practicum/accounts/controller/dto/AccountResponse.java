package ru.yandex.practicum.accounts.controller.dto;

import java.time.LocalDate;

public record AccountResponse(
        String login,
        String name,
        LocalDate birthdate,
        long balance
) {
}