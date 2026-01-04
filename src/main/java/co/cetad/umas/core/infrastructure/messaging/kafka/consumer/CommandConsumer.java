package co.cetad.umas.core.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandConsumer {

    private final EventProcessor<CommandExecutionDTO, CommandResultDTO> commandExecutionService;
    private final VehicleConnectionManager connectionManager;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @KafkaListener(
            topics = "${kafka.topics.commands}",
            groupId = "${spring.kafka.consumer.group-id:ugcs-core-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommand(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {

        try {
            var command = objectMapper.readValue(message, CommandExecutionDTO.class);
            log.debug("Parsed command: {}", command);

            ensureConnectionAndProcess(command)
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

    /**
     * Verifica la conexión con UgCS y reconecta si es necesario antes de procesar
     */
    private CompletableFuture<CommandResultDTO> ensureConnectionAndProcess(CommandExecutionDTO command) {
        return connectionManager.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        log.trace("UgCS connection active, processing command");
                        return commandExecutionService.process(command);
                    }

                    log.warn("⚠️ UgCS disconnected, attempting reconnection...");
                    return reconnectAndProcess(command);
                });
    }

    /**
     * Reconecta a UgCS y procesa el comando
     */
    private CompletableFuture<CommandResultDTO> reconnectAndProcess(CommandExecutionDTO command) {
        return connectionManager.connect()
                .then(connectionManager.subscribeTelemetry())
                .then(connectionManager.subscribeMissionComplete())
                .doOnSuccess(v -> log.info("✅ Reconnected to UgCS Server"))
                .doOnError(e -> log.error("❌ Failed to reconnect to UgCS Server", e))
                .toFuture()
                .thenCompose(v -> commandExecutionService.process(command));
    }

}