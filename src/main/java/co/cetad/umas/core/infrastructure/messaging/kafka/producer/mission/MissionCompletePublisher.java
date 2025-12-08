package co.cetad.umas.core.infrastructure.messaging.kafka.producer.mission;

import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.infrastructure.messaging.kafka.config.KafkaTopicsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Publisher de Kafka para eventos de finalización de misión
 * Publica al topic: umas.drone.mission.status
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCompletePublisher implements EventPublisher<MissionCompleteData> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public Mono<Void> publish(MissionCompleteData missionComplete) {
        return Mono.fromCallable(() -> {
                    String json = objectMapper.writeValueAsString(missionComplete);

                    log.info("📤 Publishing mission complete event - Vehicle: {}, Flight time: {} seconds",
                            missionComplete.vehicleId(),
                            missionComplete.flightTimeSeconds());

                    log.debug("Mission complete payload: {}", json);

                    return kafkaTemplate.send(
                            topics.getMissionStatus(),
                            missionComplete.vehicleId(),
                            json
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(future -> Mono.fromFuture(future.toCompletableFuture()))
                .doOnSuccess(sendResult -> log.info(
                        "✅ Mission complete event published - Topic: {}, Partition: {}, Offset: {}, Vehicle: {}",
                        sendResult.getRecordMetadata().topic(),
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset(),
                        missionComplete.vehicleId()
                ))
                .doOnError(error -> log.error(
                        "❌ Failed to publish mission complete event for vehicle: {}",
                        missionComplete.vehicleId(), error)
                )
                .then()
                .onErrorResume(error -> {
                    log.error("Error publishing mission complete event, continuing...", error);
                    return Mono.empty();
                });
    }

}