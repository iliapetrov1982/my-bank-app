package ru.yandex.practicum.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notifications.kafka.NotificationEvent;

@Slf4j
@Service
public class NotificationService {

    public void notify(NotificationEvent event) {
        log.info("[NOTIFICATION] {}", event.message());
    }
}