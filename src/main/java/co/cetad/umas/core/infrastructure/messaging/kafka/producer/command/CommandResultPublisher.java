package co.cetad.umas.core.infrastructure.messaging.kafka.producer.command;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
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
public class CommandResultPublisher implements EventPublisher<CommandResultDTO> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public Mono<Void> publish(CommandResultDTO result) {
        return Mono.fromCallable(() -> {
                    var json = objectMapper.writeValueAsString(result);

                    log.info("Publishing command result - Vehicle: {}, Command: {}, Status: {}",
                            result.vehicleId(), result.commandCode(), result.status());

                    return kafkaTemplate.send(
                            topics.getEvents(),
                            result.vehicleId(),
                            json
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(future -> Mono.fromFuture(future.toCompletableFuture()))
                .doOnSuccess(sendResult -> log.info(
                        "Command result published - Topic: {}, Partition: {}, Offset: {}",
                        sendResult.getRecordMetadata().topic(),
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset()
                ))
                .doOnError(e -> log.error("Failed to publish command result for vehicle: {} - command: {}",
                        result.vehicleId(), result.commandCode(), e))
                .then()
                .onErrorResume(e -> {
                    log.error("Error publishing command result, continuing...", e);
                    return Mono.empty();
                });
    }

}