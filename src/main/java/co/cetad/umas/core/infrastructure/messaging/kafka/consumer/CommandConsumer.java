package co.cetad.umas.core.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandConsumer {

    private final EventProcessor<CommandExecutionDTO, CommandResultDTO> commandExecutionService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @KafkaListener(
            topics = "${kafka.topics.commands}",
            groupId = "${spring.kafka.consumer.group-id:ugcs-core-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommand(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Received command - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                topic, partition, offset, key);

        try {
            var command = objectMapper.readValue(message, CommandExecutionDTO.class);
            log.debug("Parsed command: {}", command);

            commandExecutionService.process(command)
                    .thenAccept(result -> {
                        log.info("Command executed - Vehicle: {}, Status: {}, Message: {}",
                                result.vehicleId(), result.status(), result.message());
                        acknowledgment.acknowledge();
                    })
                    .exceptionally(error -> {
                        log.error("Failed to execute command from Kafka", error);
                        acknowledgment.acknowledge();
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to parse command message: {}", message, e);
            acknowledgment.acknowledge();
        }
    }

}