package co.cetad.umas.core.infrastructure.messaging.kafka.producer.telemetry;

import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.model.vo.TelemetryEvent;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryPublisher implements EventPublisher<TelemetryData> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> publish(TelemetryData telemetry) {
        return Mono.fromCallable(() -> {
                    var event = TelemetryEvent.from(telemetry);
                    var json = objectMapper.writeValueAsString(event);

                    log.trace("Publishing telemetry for vehicle: {}", telemetry.vehicleId());

                    return kafkaTemplate.send(
                            topics.getTelemetry(),
                            telemetry.vehicleId(),
                            json
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(future -> Mono.fromFuture(future.toCompletableFuture()))
                .doOnSuccess(result -> {
                    if (log.isDebugEnabled()) {
                        //SendResult<String, String> sendResult = result;
                        log.debug("Telemetry published successfully - Topic: {}, Partition: {}, Offset: {}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                })
                .doOnError(e -> log.error("Failed to publish telemetry for vehicle: {}",
                        telemetry.vehicleId(), e))
                .then()
                .onErrorResume(e -> {
                    log.error("Error publishing telemetry, continuing...", e);
                    return Mono.empty();
                });
    }

}
