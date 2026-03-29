package ru.yandex.practicum.frontui.client.dto;

import java.time.LocalDate;

public record UpdateAccountRequest(String name, LocalDate birthdate) {
}