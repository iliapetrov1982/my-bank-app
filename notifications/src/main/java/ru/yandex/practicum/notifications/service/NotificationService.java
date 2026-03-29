package ru.yandex.practicum.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notifications.controller.dto.NotificationRequest;

@Slf4j
@Service
public class NotificationService {

    public void notify(NotificationRequest request) {
        log.info("[NOTIFICATION] {}", request.message());
    }
}