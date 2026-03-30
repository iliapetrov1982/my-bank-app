package ru.yandex.practicum.notifications.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.notifications.config.SecurityConfig;
import ru.yandex.practicum.notifications.controller.dto.NotificationRequest;
import ru.yandex.practicum.notifications.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/my-bank/protocol/openid-connect/certs"
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void notify_validRequest_returns200() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt().jwt(j -> j.claim("scope", "notifications.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationRequest("Тест уведомления"))))
                .andExpect(status().isOk());

        verify(notificationService).notify(any());
    }

    @Test
    void notify_withoutJwt_returns401or403() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationRequest("Тест"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void notify_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt().jwt(j -> j.claim("scope", "notifications.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notify_missingScope_returns403() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationRequest("Тест"))))
                .andExpect(status().isForbidden());
    }
}