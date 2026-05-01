package ru.yandex.practicum.notifications.kafka;

public record NotificationEvent(String login, String message) {
}