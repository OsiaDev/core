package co.cetad.umas.core.infrastructure.messaging.kafka.producer;

import co.cetad.umas.core.domain.model.dto.VehicleStatusDTO;
import co.cetad.umas.core.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import co.cetad.umas.core.infrastructure.messaging.kafka.producer.status.VehicleStatusPublisher;
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
@DisplayName("VehicleStatusPublisher Tests")
class VehicleStatusPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    private VehicleStatusPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new VehicleStatusPublisher(kafkaTemplate, topics);
    }

    @Nested
    @DisplayName("Successful publish tests")
    class SuccessfulPublishTests {

        @Test
        @DisplayName("Should publish vehicle connected status successfully")
        void shouldPublishVehicleConnectedStatusSuccessfully() {
            var status = VehicleStatusDTO.connected("vehicle-1");
            var sendResult = createMockSendResult("umas.drone.vehicle.status", 0, 100L);

            when(topics.getVehicleStatus()).thenReturn("umas.drone.vehicle.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.notify(status))
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("umas.drone.vehicle.status"), eq("vehicle-1"), any());
        }

        @Test
        @DisplayName("Should publish vehicle error status successfully")
        void shouldPublishVehicleErrorStatusSuccessfully() {
            var status = VehicleStatusDTO.error("vehicle-1", "Connection lost");
            var sendResult = createMockSendResult("umas.drone.vehicle.status", 0, 100L);

            when(topics.getVehicleStatus()).thenReturn("umas.drone.vehicle.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.notify(status))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle publish error gracefully")
        void shouldHandlePublishErrorGracefully() {
            var status = VehicleStatusDTO.connected("vehicle-1");

            when(topics.getVehicleStatus()).thenReturn("umas.drone.vehicle.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

            StepVerifier.create(publisher.notify(status))
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
