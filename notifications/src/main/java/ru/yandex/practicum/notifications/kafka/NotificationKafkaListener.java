package ru.yandex.practicum.notifications.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.notifications.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.notifications-topic}", groupId = "notifications-group")
    public void onNotification(NotificationEvent event) {
        notificationService.notify(event);
    }
}