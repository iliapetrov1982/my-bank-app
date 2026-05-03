package ru.yandex.practicum.transfer.kafka;

public record NotificationEvent(String login, String message) {
}