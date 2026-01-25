package co.cetad.umas.core.infrastructure.messaging.kafka.producer;

import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import co.cetad.umas.core.infrastructure.messaging.kafka.producer.mission.MissionCompletePublisher;
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
@DisplayName("MissionCompletePublisher Tests")
class MissionCompletePublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaTopicsProperties topics;

    private MissionCompletePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new MissionCompletePublisher(kafkaTemplate, topics);
    }

    @Nested
    @DisplayName("Successful publish tests")
    class SuccessfulPublishTests {

        @Test
        @DisplayName("Should publish mission complete event successfully")
        void shouldPublishMissionCompleteEventSuccessfully() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: 100.5",
                    System.currentTimeMillis()
            );
            var sendResult = createMockSendResult("umas.drone.mission.status", 0, 100L);

            when(topics.getMissionStatus()).thenReturn("umas.drone.mission.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.publish(missionComplete))
                    .verifyComplete();

            verify(kafkaTemplate).send(eq("umas.drone.mission.status"), eq("vehicle-1"), any());
        }

        @Test
        @DisplayName("Should publish mission complete with all fields")
        void shouldPublishMissionCompleteWithAllFields() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "drone-1",
                    "Current mission complete. Flight time: 93.5",
                    System.currentTimeMillis()
            ).withMissionId("mission-123");
            var sendResult = createMockSendResult("umas.drone.mission.status", 0, 100L);

            when(topics.getMissionStatus()).thenReturn("umas.drone.mission.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(sendResult));

            StepVerifier.create(publisher.publish(missionComplete))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle publish error gracefully")
        void shouldHandlePublishErrorGracefully() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(topics.getMissionStatus()).thenReturn("umas.drone.mission.status");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka error")));

            StepVerifier.create(publisher.publish(missionComplete))
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
