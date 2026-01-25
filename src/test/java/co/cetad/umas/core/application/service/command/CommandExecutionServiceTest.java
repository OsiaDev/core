package co.cetad.umas.core.application.service.command;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandExecutionService Tests")
class CommandExecutionServiceTest {

    @Mock
    private UgcsClient ugcsClient;

    @Mock
    private EventPublisher<CommandResultDTO> commandResultPublisher;

    @Mock
    private CommandValidator commandValidator;

    private CommandExecutionService service;

    @BeforeEach
    void setUp() {
        service = new CommandExecutionService(ugcsClient, commandResultPublisher, commandValidator);
    }

    @Nested
    @DisplayName("Successful command execution tests")
    class SuccessfulExecutionTests {

        @Test
        @DisplayName("Should execute command successfully")
        void shouldExecuteCommandSuccessfully() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(commandResultPublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(command).get();

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("arm", result.commandCode());
            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
        }

        @Test
        @DisplayName("Should return failed result when executeCommand returns false")
        void shouldReturnFailedResultWhenExecuteCommandReturnsFalse() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(false));
            when(commandResultPublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(command).get();

            assertEquals(CommandResultDTO.CommandStatus.FAILED, result.status());
            assertTrue(result.message().contains("returned false"));
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return rejected status when not connected")
        void shouldReturnRejectedStatusWhenNotConnected() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(false));

            var result = service.process(command).get();

            assertEquals(CommandResultDTO.CommandStatus.REJECTED, result.status());
        }

        @Test
        @DisplayName("Should return failed status when validation fails")
        void shouldReturnFailedStatusWhenValidationFails() throws Exception {
            var command = createCommand("invalid", Map.of());

            when(commandValidator.validate(any()))
                    .thenReturn(CompletableFuture.failedFuture(
                            new IllegalArgumentException("Invalid command")));

            var result = service.process(command).get();

            assertEquals(CommandResultDTO.CommandStatus.REJECTED, result.status());
        }

        @Test
        @DisplayName("Should return failed status when UgCS execution fails")
        void shouldReturnFailedStatusWhenUgcsExecutionFails() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("UgCS error")));

            var result = service.process(command).get();

            assertEquals(CommandResultDTO.CommandStatus.FAILED, result.status());
        }

        @Test
        @DisplayName("Should return timeout status on timeout")
        void shouldReturnTimeoutStatusOnTimeout() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Command timed out")));

            var result = service.process(command).get();

            assertEquals(CommandResultDTO.CommandStatus.TIMEOUT, result.status());
        }
    }

    @Nested
    @DisplayName("Publisher interaction tests")
    class PublisherInteractionTests {

        @Test
        @DisplayName("Should publish result on successful execution")
        void shouldPublishResultOnSuccessfulExecution() throws Exception {
            var command = createCommand("arm", Map.of());

            when(commandValidator.validate(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(commandResultPublisher.publish(any())).thenReturn(Mono.empty());

            service.process(command).get();

            verify(commandResultPublisher).publish(any(CommandResultDTO.class));
        }
    }

    private CommandExecutionDTO createCommand(String commandCode, Map<String, Double> arguments) {
        return new CommandExecutionDTO("vehicle-1", "mission-1", commandCode, arguments, 1);
    }
}
