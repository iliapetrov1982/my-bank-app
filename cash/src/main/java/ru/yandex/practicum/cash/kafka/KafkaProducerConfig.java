package ru.yandex.practicum.cash.kafka;

import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.notifications-topic}")
    private String notificationsTopic;

    @Bean
    public ProducerFactory<String, NotificationEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class
        ));
    }

    @Bean
    public KafkaTemplate<String, NotificationEvent> kafkaTemplate(
            ProducerFactory<String, NotificationEvent> producerFactory,
            ObservationRegistry observationRegistry) {
        KafkaTemplate<String, NotificationEvent> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        template.setObservationRegistry(observationRegistry);
        return template;
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(notificationsTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
