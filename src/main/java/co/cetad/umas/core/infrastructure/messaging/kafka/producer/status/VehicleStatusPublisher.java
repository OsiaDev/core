package co.cetad.umas.core.infrastructure.messaging.kafka.producer.status;

import co.cetad.umas.core.domain.model.dto.VehicleStatusDTO;
import co.cetad.umas.core.domain.ports.out.StatusNotifier;
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
public class VehicleStatusPublisher implements StatusNotifier {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public Mono<Void> notify(VehicleStatusDTO status) {
        return Mono.fromCallable(() -> {
                    var json = objectMapper.writeValueAsString(status);

                    log.debug("Publishing vehicle status for: {} - State: {}",
                            status.vehicleId(), status.state());

                    return kafkaTemplate.send(
                            topics.getVehicleStatus(),
                            status.vehicleId(),
                            json
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(future -> Mono.fromFuture(future.toCompletableFuture()))
                .doOnSuccess(result -> log.debug(
                        "Vehicle status published - Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                ))
                .doOnError(e -> log.error("Failed to publish vehicle status for: {}",
                        status.vehicleId(), e))
                .then()
                .onErrorResume(e -> {
                    log.error("Error publishing vehicle status, continuing...", e);
                    return Mono.empty();
                });
    }

}