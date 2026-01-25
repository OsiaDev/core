package co.cetad.umas.core.infrastructure.messaging.kafka.consumer;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
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
@DisplayName("MissionExecutionConsumer Tests")
class MissionExecutionConsumerTest {

    @Mock
    private EventProcessor<MissionExecutionDTO, CommandResultDTO> missionExecutionService;

    @Mock
    private VehicleConnectionManager connectionManager;

    @Mock
    private Acknowledgment acknowledgment;

    private MissionExecutionConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MissionExecutionConsumer(missionExecutionService, connectionManager);
    }

    @Nested
    @DisplayName("Successful consumption tests")
    class SuccessfulConsumptionTests {

        @Test
        @DisplayName("Should consume and process mission when connected")
        void shouldConsumeAndProcessMissionWhenConnected() throws Exception {
            var message = """
                {
                    "missionId": "mission-1",
                    "drones": [
                        {
                            "vehicleId": "drone-1",
                            "routeId": "route-1",
                            "safeAltitude": 50.0,
                            "maxAltitude": 100.0,
                            "waypoints": [
                                {"latitude": 45.0, "longitude": -73.0}
                            ]
                        }
                    ],
                    "priority": 1
                }
                """;
            var result = CommandResultDTO.success("mission", "execute_mission");

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(missionExecutionService.process(any())).thenReturn(CompletableFuture.completedFuture(result));

            consumer.consumeMissionExecution(message, acknowledgment);

            Thread.sleep(100);
            verify(acknowledgment).acknowledge();
            verify(missionExecutionService).process(any(MissionExecutionDTO.class));
        }

        @Test
        @DisplayName("Should reconnect when not connected and process mission")
        void shouldReconnectWhenNotConnectedAndProcessMission() throws Exception {
            var message = """
                {
                    "missionId": "mission-1",
                    "drones": [
                        {
                            "vehicleId": "drone-1",
                            "routeId": "route-1",
                            "safeAltitude": 50.0,
                            "maxAltitude": 100.0,
                            "waypoints": []
                        }
                    ],
                    "priority": 1
                }
                """;
            var result = CommandResultDTO.success("mission", "execute_mission");

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(false));
            when(connectionManager.connect()).thenReturn(Mono.empty());
            when(connectionManager.subscribeTelemetry()).thenReturn(Mono.empty());
            when(connectionManager.subscribeMissionComplete()).thenReturn(Mono.empty());
            when(missionExecutionService.process(any())).thenReturn(CompletableFuture.completedFuture(result));

            consumer.consumeMissionExecution(message, acknowledgment);

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

            consumer.consumeMissionExecution(invalidMessage, acknowledgment);

            Thread.sleep(100);
            verify(acknowledgment).acknowledge();
            verifyNoInteractions(missionExecutionService);
        }

        @Test
        @DisplayName("Should acknowledge on processing error")
        void shouldAcknowledgeOnProcessingError() throws Exception {
            var message = """
                {
                    "missionId": "mission-1",
                    "drones": [
                        {
                            "vehicleId": "drone-1",
                            "routeId": "route-1",
                            "safeAltitude": 50.0,
                            "maxAltitude": 100.0,
                            "waypoints": []
                        }
                    ],
                    "priority": 1
                }
                """;

            when(connectionManager.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(missionExecutionService.process(any()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing error")));

            consumer.consumeMissionExecution(message, acknowledgment);

            Thread.sleep(100);
            verify(acknowledgment).acknowledge();
        }
    }
}
