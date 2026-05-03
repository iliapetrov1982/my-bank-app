package ru.yandex.practicum.accounts.kafka;

public record NotificationEvent(String login, String message) {
}