package co.cetad.umas.core.application.service.command;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandExecutionService implements EventProcessor<CommandExecutionDTO, CommandResultDTO> {

    private final UgcsClient ugcsClient;
    private final EventPublisher<CommandResultDTO> commandResultPublisher;
    private final CommandValidator commandValidator;

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(30);

    @Override
    public CompletableFuture<CommandResultDTO> process(CommandExecutionDTO command) {
        log.info("Processing command: {} for vehicle: {}",
                command.commandCode(), command.vehicleId());

        return commandValidator.validate(command)
                .thenCompose(v -> validateConnection())
                .thenCompose(v -> executeInUgcs(command))
                .thenApply(success -> buildResult(command, success))
                .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        handleError(command, error);
                    } else {
                        publishResult(result);
                    }
                })
                .exceptionally(error -> buildErrorResult(command, error));
    }

    private CompletableFuture<Void> validateConnection() {
        return ugcsClient.isConnected()
                .thenAccept(connected -> {
                    if (!connected) {
                        throw new IllegalStateException("Not connected to UgCS Server");
                    }
                });
    }

    private CompletableFuture<Boolean> executeInUgcs(CommandExecutionDTO dto) {
        var commandRequest = new CommandRequest(
                dto.vehicleId(),
                dto.commandCode(),
                dto.arguments()
        );

        return ugcsClient.executeCommand(commandRequest)
                .whenComplete((success, error) -> {
                    if (error != null) {
                        log.error("Command execution failed - Vehicle: {}, Command: {}",
                                dto.vehicleId(), dto.commandCode(), error);
                    } else {
                        log.info("Command execution completed - Vehicle: {}, Command: {}, Success: {}",
                                dto.vehicleId(), dto.commandCode(), success);
                    }
                });
    }

    private CommandResultDTO buildResult(CommandExecutionDTO dto, boolean success) {
        return success
                ? CommandResultDTO.success(dto.vehicleId(), dto.commandCode())
                : new CommandResultDTO(
                dto.vehicleId(),
                dto.commandCode(),
                CommandResultDTO.CommandStatus.FAILED,
                "Command execution returned false",
                Instant.now()
        );
    }

    private CommandResultDTO buildErrorResult(CommandExecutionDTO dto, Throwable error) {
        log.error("Error executing command: {} for vehicle: {}",
                dto.commandCode(), dto.vehicleId(), error);

        var status = determineErrorStatus(error);
        return new CommandResultDTO(
                dto.vehicleId(),
                dto.commandCode(),
                status,
                error.getMessage(),
                Instant.now()
        );
    }

    private void handleError(CommandExecutionDTO dto, Throwable error) {
        log.error("Failed to execute command: {} for vehicle: {}",
                dto.commandCode(), dto.vehicleId(), error);
    }

    private CommandResultDTO.CommandStatus determineErrorStatus(Throwable error) {
        if (error instanceof TimeoutException) {
            return CommandResultDTO.CommandStatus.TIMEOUT;
        }
        if (error instanceof IllegalStateException) {
            return CommandResultDTO.CommandStatus.REJECTED;
        }
        return CommandResultDTO.CommandStatus.FAILED;
    }

    private void publishResult(CommandResultDTO result) {
        commandResultPublisher.publish(result)
                .doOnError(e -> log.error("Failed to publish command result", e))
                .subscribe();
    }

}