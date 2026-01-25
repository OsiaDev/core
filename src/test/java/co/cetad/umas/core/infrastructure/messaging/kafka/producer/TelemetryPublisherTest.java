package co.cetad.umas.core.infrastructure.messaging.kafka.producer;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import co.cetad.umas.core.infrastructure.messaging.kafka.producer.telemetry.TelemetryPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryPublisher Tests")
class TelemetryPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    private ObjectMapper objectMapper;
    private TelemetryPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        publisher = new TelemetryPublisher(kafkaTemplate, topics, objectMapper);
    }

    @Nested
    @DisplayName("Successful publish tests")
    class SuccessfulPublishTests {

        @Test
        @DisplayName("Should publish telemetry successfully")
        void shouldPublishTelemetrySuccessfully() {
            var telemetry = createTelemetry("vehicle-1", 45.0, -73.0, 100.0);
            var sendResult = createMockSendResult("umas.drone.telemetry", 0, 100L);

            when(topics.getTelemetry()).thenReturn("umas.drone.telemetry");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.publish(telemetry))
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("umas.drone.telemetry"), eq("vehicle-1"), any());
        }

        @Test
        @DisplayName("Should publish telemetry with all fields")
        void shouldPublishTelemetryWithAllFields() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of(
                    "groundSpeed", 15.5,
                    "batteryLevel", 85.0,
                    "heading", 270.0,
                    "satelliteCount", 12
            );
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());
            var sendResult = createMockSendResult("umas.drone.telemetry", 0, 100L);

            when(topics.getTelemetry()).thenReturn("umas.drone.telemetry");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.publish(telemetry))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle publish error gracefully")
        void shouldHandlePublishErrorGracefully() {
            var telemetry = createTelemetry("vehicle-1", 45.0, -73.0, 100.0);

            when(topics.getTelemetry()).thenReturn("umas.drone.telemetry");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

            StepVerifier.create(publisher.publish(telemetry))
                    .verifyComplete();  // Error is handled, so it completes
        }
    }

    private TelemetryData createTelemetry(String vehicleId, double lat, double lon, double alt) {
        var location = DroneLocation.of(lat, lon, alt);
        return new TelemetryData(vehicleId, location, Map.of(), LocalDateTime.now());
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, String> createMockSendResult(String topic, int partition, long offset) {
        var topicPartition = new TopicPartition(topic, partition);
        var recordMetadata = new RecordMetadata(topicPartition, offset, 0, 0L, 0, 0);
        var producerRecord = new ProducerRecord<String, String>(topic, "key", "value");
        return new SendResult<>(producerRecord, recordMetadata);
    }
}
