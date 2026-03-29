package ru.yandex.practicum.transfer.controller.dto;

public record TransferResponse(String fromLogin, String toLogin, long amount) {
}