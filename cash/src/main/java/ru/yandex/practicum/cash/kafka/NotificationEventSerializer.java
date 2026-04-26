package ru.yandex.practicum.cash.kafka;

import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.ObjectMapper;

public class NotificationEventSerializer implements Serializer<NotificationEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, NotificationEvent data) {
        if (data == null) {
            return null;
        }
        return objectMapper.writeValueAsBytes(data);
    }
}
