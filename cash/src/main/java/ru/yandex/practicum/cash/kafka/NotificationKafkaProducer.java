package ru.yandex.practicum.cash.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void send(NotificationEvent event) {
        kafkaTemplate.send("notifications", event);
    }
}