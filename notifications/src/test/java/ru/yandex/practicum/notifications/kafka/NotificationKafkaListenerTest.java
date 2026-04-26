package ru.yandex.practicum.notifications.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.notifications.service.NotificationService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "notifications")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class NotificationKafkaListenerTest {

    @TestConfiguration
    static class KafkaProducerTestConfig {

        @Bean
        public ProducerFactory<String, String> producerFactory(EmbeddedKafkaBroker broker) {
            return new DefaultKafkaProducerFactory<>(Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
            ));
        }

        @Bean
        public KafkaTemplate<String, String> kafkaTemplate(
                ProducerFactory<String, String> producerFactory) {
            return new KafkaTemplate<>(producerFactory);
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void onNotification_receivesAndProcessesMessage() throws Exception {
        registry.getListenerContainers()
                .forEach(c -> ContainerTestUtils.waitForAssignment(c, embeddedKafka.getPartitionsPerTopic()));

        String json = "{\"message\":\"Тестовое уведомление\"}";

        kafkaTemplate.send("notifications", json).get(10, TimeUnit.SECONDS);

        verify(notificationService, timeout(10000))
                .notify(argThat(e -> e.message().equals("Тестовое уведомление")));
    }
}
