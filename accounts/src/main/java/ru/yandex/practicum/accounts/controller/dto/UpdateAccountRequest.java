package ru.yandex.practicum.accounts.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdateAccountRequest(

        @NotBlank
        String name,

        @NotNull
        @Past
        LocalDate birthdate
) {
}