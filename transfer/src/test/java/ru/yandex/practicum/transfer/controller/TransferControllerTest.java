package ru.yandex.practicum.transfer.controller;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.transfer.config.SecurityConfig;
import ru.yandex.practicum.transfer.controller.dto.TransferRequest;
import ru.yandex.practicum.transfer.controller.dto.TransferResponse;
import ru.yandex.practicum.transfer.service.TransferService;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/my-bank/protocol/openid-connect/certs"
})
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    @Test
    void transfer_validRequest_returns200() throws Exception {
        when(transferService.transfer(eq("ivan"), any()))
                .thenReturn(new TransferResponse("ivan", "petrov", 100L));

        mockMvc.perform(post("/api/transfer")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "transfer.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest("petrov", 100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromLogin", equalTo("ivan")))
                .andExpect(jsonPath("$.toLogin", equalTo("petrov")))
                .andExpect(jsonPath("$.amount", equalTo(100)));
    }

    @Test
    void transfer_withoutJwt_returns401or403() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest("petrov", 100L))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void transfer_wrongScope_returns403() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "accounts.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest("petrov", 100L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_blankToLogin_returns400() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "transfer.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest("", 100L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_negativeAmount_returns400() throws Exception {
        mockMvc.perform(post("/api/transfer")
                        .with(jwt().jwt(j -> j
                                .claim("preferred_username", "ivan")
                                .claim("scope", "transfer.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferRequest("petrov", -50L))))
                .andExpect(status().isBadRequest());
    }
}