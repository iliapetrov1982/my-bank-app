package ru.yandex.practicum.accounts.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${app.kafka.notifications-topic}")
    private String topic;

    public void send(NotificationEvent event) {
        kafkaTemplate.send(topic, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send notification to topic {}: {}", topic, event, ex);
            }
        });
    }
}
