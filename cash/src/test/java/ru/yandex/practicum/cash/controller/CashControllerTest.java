package ru.yandex.practicum.cash.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.cash.config.SecurityConfig;
import ru.yandex.practicum.cash.controller.dto.CashRequest;
import ru.yandex.practicum.cash.controller.dto.CashResponse;
import ru.yandex.practicum.cash.service.CashService;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/my-bank/protocol/openid-connect/certs"
})
class CashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CashService cashService;

    // --- POST /api/cash/deposit ---

    @Test
    void deposit_validJwtAndAmount_returns200() throws Exception {
        when(cashService.deposit(eq("ivan"), any()))
                .thenReturn(new CashResponse("ivan", 1200L));

        mockMvc.perform(post("/api/cash/deposit")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "cash.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(200L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login", equalTo("ivan")))
                .andExpect(jsonPath("$.balance", equalTo(1200)));
    }

    @Test
    void deposit_withoutJwt_returns401or403() throws Exception {
        mockMvc.perform(post("/api/cash/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(200L))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deposit_wrongScope_returns403() throws Exception {
        mockMvc.perform(post("/api/cash/deposit")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "accounts.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(200L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deposit_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/cash/deposit")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "cash.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(-100L))))
                .andExpect(status().isBadRequest());
    }

    // --- POST /api/cash/withdraw ---

    @Test
    void withdraw_validJwtAndAmount_returns200() throws Exception {
        when(cashService.withdraw(eq("ivan"), any()))
                .thenReturn(new CashResponse("ivan", 800L));

        mockMvc.perform(post("/api/cash/withdraw")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "cash.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(200L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", equalTo(800)));
    }
}