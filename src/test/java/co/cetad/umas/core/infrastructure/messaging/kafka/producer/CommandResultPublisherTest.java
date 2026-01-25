package co.cetad.umas.core.infrastructure.messaging.kafka.producer;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import co.cetad.umas.core.infrastructure.messaging.kafka.producer.command.CommandResultPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandResultPublisher Tests")
class CommandResultPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    private CommandResultPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CommandResultPublisher(kafkaTemplate, topics);
    }

    @Nested
    @DisplayName("Successful publish tests")
    class SuccessfulPublishTests {

        @Test
        @DisplayName("Should publish command result successfully")
        void shouldPublishCommandResultSuccessfully() {
            var result = CommandResultDTO.success("vehicle-1", "arm");
            var sendResult = createMockSendResult("umas.drone.events", 0, 100L);

            when(topics.getEvents()).thenReturn("umas.drone.events");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.publish(result))
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("umas.drone.events"), eq("vehicle-1"), any());
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle publish error gracefully")
        void shouldHandlePublishErrorGracefully() {
            var result = CommandResultDTO.success("vehicle-1", "arm");

            when(topics.getEvents()).thenReturn("umas.drone.events");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

            StepVerifier.create(publisher.publish(result))
                    .verifyComplete();  // Error is handled, so it completes
        }
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, String> createMockSendResult(String topic, int partition, long offset) {
        var topicPartition = new TopicPartition(topic, partition);
        var recordMetadata = new RecordMetadata(topicPartition, offset, 0, 0L, 0, 0);
        var producerRecord = new ProducerRecord<String, String>(topic, "key", "value");
        return new SendResult<>(producerRecord, recordMetadata);
    }
}
