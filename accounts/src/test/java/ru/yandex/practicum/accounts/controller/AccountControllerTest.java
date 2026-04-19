package ru.yandex.practicum.accounts.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.accounts.config.SecurityConfig;
import ru.yandex.practicum.accounts.controller.dto.AccountResponse;
import ru.yandex.practicum.accounts.controller.dto.AccountShortResponse;
import ru.yandex.practicum.accounts.controller.dto.BalanceOperationRequest;
import ru.yandex.practicum.accounts.service.AccountService;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/my-bank/protocol/openid-connect/certs"
})
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    private static final AccountResponse IVAN_RESPONSE = new AccountResponse(
            "ivan", "Иванов Иван", LocalDate.of(1990, 1, 25), 1000L
    );

    // --- GET /api/accounts/me ---

    @Test
    void getMyAccount_withValidJwt_returns200() throws Exception {
        when(accountService.getAccount("ivan")).thenReturn(IVAN_RESPONSE);

        mockMvc.perform(get("/api/accounts/me")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "accounts.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", equalTo("ivan")))
                .andExpect(jsonPath("$.balance", equalTo(1000)));
    }

    @Test
    void getMyAccount_withoutJwt_returns401or403() throws Exception {
        mockMvc.perform(get("/api/accounts/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMyAccount_withWrongScope_returns403() throws Exception {
        mockMvc.perform(get("/api/accounts/me")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "cash.write"))))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/accounts ---

    @Test
    void getOtherAccounts_withValidJwt_returnsList() throws Exception {
        when(accountService.getOtherAccounts("ivan"))
                .thenReturn(List.of(new AccountShortResponse("petrov", "Петров Пётр")));

        mockMvc.perform(get("/api/accounts")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "accounts.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].login", equalTo("petrov")));
    }

    // --- POST /api/accounts/{login}/deposit ---

    @Test
    void deposit_validRequest_returns200() throws Exception {
        when(accountService.deposit(eq("ivan"), any())).thenReturn(IVAN_RESPONSE);

        mockMvc.perform(post("/api/accounts/ivan/deposit")
                        .with(jwt().jwt(j -> j.claim("scope", "accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceOperationRequest(200L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", equalTo("ivan")));
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/accounts/ivan/deposit")
                        .with(jwt().jwt(j -> j.claim("scope", "accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceOperationRequest(-100L))))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/accounts/{login}/withdraw ---

    @Test
    void withdraw_validRequest_returns200() throws Exception {
        when(accountService.withdraw(eq("ivan"), any())).thenReturn(IVAN_RESPONSE);

        mockMvc.perform(post("/api/accounts/ivan/withdraw")
                        .with(jwt().jwt(j -> j.claim("scope", "accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceOperationRequest(100L))))
                .andExpect(status().isOk());
    }

    @Test
    void withdraw_accountNotFound_returns404() throws Exception {
        when(accountService.withdraw(eq("unknown"), any()))
                .thenThrow(new NoSuchElementException("Аккаунт не найден: unknown"));

        mockMvc.perform(post("/api/accounts/unknown/withdraw")
                        .with(jwt().jwt(j -> j.claim("scope", "accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceOperationRequest(100L))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void withdraw_insufficientFunds_returns400() throws Exception {
        when(accountService.withdraw(eq("ivan"), any()))
                .thenThrow(new IllegalStateException("Недостаточно средств на счету"));

        mockMvc.perform(post("/api/accounts/ivan/withdraw")
                        .with(jwt().jwt(j -> j.claim("scope", "accounts.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BalanceOperationRequest(9999L))))
                .andExpect(status().isBadRequest());
    }
}