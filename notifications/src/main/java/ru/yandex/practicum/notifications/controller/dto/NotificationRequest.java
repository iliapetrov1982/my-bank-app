package ru.yandex.practicum.notifications.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(@NotBlank String message) {
}