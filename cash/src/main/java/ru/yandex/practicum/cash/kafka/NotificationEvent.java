package ru.yandex.practicum.cash.kafka;

public record NotificationEvent(String login, String message) {
}