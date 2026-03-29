package ru.yandex.practicum.frontui.client.dto;

public record TransferResponse(String fromLogin, String toLogin, long amount) {
}