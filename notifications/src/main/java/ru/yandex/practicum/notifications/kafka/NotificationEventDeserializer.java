package ru.yandex.practicum.notifications.kafka;

import org.apache.kafka.common.serialization.Deserializer;
import tools.jackson.databind.ObjectMapper;

public class NotificationEventDeserializer implements Deserializer<NotificationEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NotificationEvent deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        return objectMapper.readValue(data, NotificationEvent.class);
    }
}
