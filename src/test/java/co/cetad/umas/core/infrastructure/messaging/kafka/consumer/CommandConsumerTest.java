package co.cetad.umas.core.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandConsumer Tests")
class CommandConsumerTest {

    @Mock
    private EventProcessor<CommandExecutionDTO, CommandResultDTO> commandExecutionService;

    @Mock
    private VehicleConnectionManager connectionManager;

    @Mock
    private Acknowledgment acknowledgment;

    private CommandConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CommandConsumer(commandExecutionService, connectionManager);
    }

    @Nested
    @DisplayName("Successful consumption tests")
    class SuccessfulConsumptionTests {

        @Test
        @DisplayName("Should consume and process command when connected")
        void shouldConsumeAndProcessCommandWhenConnected() throws Exception {
            var message = """
                {
                    "vehicleId": "vehicle-1",
                    "routeId": "mission-1",
                    "commandCode": "arm",
                    "arguments": {},
                    "priority": 1
                }
                """;
            var result = CommandResultDTO.success("vehicle-1", "arm");

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(commandExecutionService.process(any())).thenReturn(CompletableFuture.completedFuture(result));

            consumer.consumeCommand(message, acknowledgment);

            Thread.sleep(100); // Wait for async processing
            verify(acknowledgment).acknowledge();
            verify(commandExecutionService).process(any(CommandExecutionDTO.class));
        }

        @Test
        @DisplayName("Should reconnect when not connected and process command")
        void shouldReconnectWhenNotConnectedAndProcessCommand() throws Exception {
            var message = """
                {
                    "vehicleId": "vehicle-1",
                    "routeId": "mission-1",
                    "commandCode": "arm",
                    "arguments": {},
                    "priority": 1
                }
                """;
            var result = CommandResultDTO.success("vehicle-1", "arm");

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(false));
            when(connectionManager.connect()).thenReturn(Mono.empty());
            when(connectionManager.subscribeTelemetry()).thenReturn(Mono.empty());
            when(connectionManager.subscribeMissionComplete()).thenReturn(Mono.empty());
            when(commandExecutionService.process(any())).thenReturn(CompletableFuture.completedFuture(result));

            consumer.consumeCommand(message, acknowledgment);

            Thread.sleep(100);
            verify(connectionManager).connect();
            verify(connectionManager).subscribeTelemetry();
            verify(connectionManager).subscribeMissionComplete();
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should acknowledge on parse error")
        void shouldAcknowledgeOnParseError() throws Exception {
            var invalidMessage = "invalid json";

            consumer.consumeCommand(invalidMessage, acknowledgment);

            Thread.sleep(100);
            verify(acknowledgment).acknowledge();
            verifyNoInteractions(commandExecutionService);
        }

        @Test
        @DisplayName("Should acknowledge on processing error")
        void shouldAcknowledgeOnProcessingError() throws Exception {
            var message = """
                {
                    "vehicleId": "vehicle-1",
                    "routeId": "mission-1",
                    "commandCode": "arm",
                    "arguments": {},
                    "priority": 1
                }
                """;

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(commandExecutionService.process(any()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing error")));

            consumer.consumeCommand(message, acknowledgment);

            Thread.sleep(100);
            verify(acknowledgment).acknowledge();
        }
    }
}
