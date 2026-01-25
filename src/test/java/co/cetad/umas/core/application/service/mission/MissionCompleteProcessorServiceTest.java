package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
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

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MissionCompleteProcessorService Tests")
class MissionCompleteProcessorServiceTest {

    @Mock
    private UgcsClient ugcsClient;

    @Mock
    private EventPublisher<MissionCompleteData> missionCompletePublisher;

    private MissionCompleteProcessorService service;

    @BeforeEach
    void setUp() {
        service = new MissionCompleteProcessorService(ugcsClient, missionCompletePublisher);
    }

    @Nested
    @DisplayName("Successful processing tests")
    class SuccessfulProcessingTests {

        @Test
        @DisplayName("Should process mission complete event successfully")
        void shouldProcessMissionCompleteEventSuccessfully() throws Exception {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: 100.5",
                    System.currentTimeMillis()
            );

            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(missionCompletePublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(missionComplete).get();

            assertNull(result);
            verify(ugcsClient).executeCommand(any(CommandRequest.class));
            verify(missionCompletePublisher).publish(missionComplete);
        }

        @Test
        @DisplayName("Should send LAND command with correct parameters")
        void shouldSendLandCommandWithCorrectParameters() throws Exception {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(missionCompletePublisher.publish(any())).thenReturn(Mono.empty());

            service.process(missionComplete).get();

            verify(ugcsClient).executeCommand(argThat(cmd ->
                    cmd.vehicleId().equals("vehicle-1") &&
                            cmd.commandCode().equals("land_command")
            ));
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should continue with publish even if LAND command fails")
        void shouldContinueWithPublishEvenIfLandCommandFails() throws Exception {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(false));
            when(missionCompletePublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(missionComplete).get();

            assertNull(result);
            verify(missionCompletePublisher).publish(missionComplete);
        }

        @Test
        @DisplayName("Should handle LAND command exception and continue")
        void shouldHandleLandCommandExceptionAndContinue() throws Exception {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Command error")));
            when(missionCompletePublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(missionComplete).get();

            assertNull(result);
            verify(missionCompletePublisher).publish(missionComplete);
        }

        @Test
        @DisplayName("Should handle publish error gracefully")
        void shouldHandlePublishErrorGracefully() throws Exception {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(missionCompletePublisher.publish(any()))
                    .thenReturn(Mono.error(new RuntimeException("Publish error")));

            var result = service.process(missionComplete).get();

            assertNull(result);
        }
    }
}
