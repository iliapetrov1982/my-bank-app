package ru.yandex.practicum.notifications.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.notifications.kafka.NotificationEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MeterRegistry meterRegistry;

    public void notify(NotificationEvent event) {
        try {
            log.info("[NOTIFICATION] login={} message={}", event.login(), event.message());
        } catch (RuntimeException e) {
            log.error("Failed to deliver notification for login={}: {}", event.login(), event.message(), e);
            meterRegistry.counter("notification.send.failed", "login", event.login()).increment();
            throw e;
        }
    }
}