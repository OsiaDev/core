package co.cetad.umas.core.application.service.ugcs;

import co.cetad.umas.core.application.service.mission.MissionCompleteProcessorService;
import co.cetad.umas.core.application.service.telemetry.TelemetryProcessorService;
import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.StatusNotifier;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import co.cetad.umas.core.infrastructure.ugcs.config.UgcsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UgcsConnectionService Tests")
class UgcsConnectionServiceTest {

    @Mock
    private UgcsClient ugcsClient;

    @Mock
    private StatusNotifier statusNotifier;

    @Mock
    private TelemetryProcessorService telemetryProcessorService;

    @Mock
    private MissionCompleteProcessorService missionCompleteProcessorService;

    private UgcsProperties properties;
    private UgcsConnectionService service;

    @BeforeEach
    void setUp() {
        properties = new UgcsProperties();
        properties.getServer().setHost("localhost");
        properties.getServer().setPort(3334);
        properties.getCredentials().setLogin("admin");
        properties.getCredentials().setPassword("admin");
        properties.getReconnect().setEnabled(true);
        properties.getReconnect().setInitialDelay(1000);
        properties.getReconnect().setMaxDelay(5000);

        service = new UgcsConnectionService(
                ugcsClient,
                statusNotifier,
                telemetryProcessorService,
                missionCompleteProcessorService,
                properties
        );
    }

    @Nested
    @DisplayName("Connect tests")
    class ConnectTests {

        @Test
        @DisplayName("Should connect successfully")
        void shouldConnectSuccessfully() {
            when(ugcsClient.connect(anyString(), anyInt(), anyString(), anyString()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(service.connect())
                    .verifyComplete();

            verify(ugcsClient).connect("localhost", 3334, "admin", "admin");
        }
    }

    @Nested
    @DisplayName("Disconnect tests")
    class DisconnectTests {

        @Test
        @DisplayName("Should disconnect successfully")
        void shouldDisconnectSuccessfully() {
            when(ugcsClient.disconnect()).thenReturn(Mono.empty());

            StepVerifier.create(service.disconnect())
                    .verifyComplete();

            verify(ugcsClient).disconnect();
        }

        @Test
        @DisplayName("Should handle disconnect error")
        void shouldHandleDisconnectError() {
            when(ugcsClient.disconnect())
                    .thenReturn(Mono.error(new RuntimeException("Disconnect error")));

            StepVerifier.create(service.disconnect())
                    .verifyError(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("isConnected tests")
    class IsConnectedTests {

        @Test
        @DisplayName("Should return connected status")
        void shouldReturnConnectedStatus() throws Exception {
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));

            var result = service.isConnected().get();

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return disconnected status")
        void shouldReturnDisconnectedStatus() throws Exception {
            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(false));

            var result = service.isConnected().get();

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Subscribe telemetry tests")
    class SubscribeTelemetryTests {

        @Test
        @DisplayName("Should subscribe to telemetry")
        void shouldSubscribeToTelemetry() {
            var telemetry = new TelemetryData(
                    "vehicle-1",
                    DroneLocation.of(45.0, -73.0, 100.0),
                    Map.of(),
                    LocalDateTime.now()
            );

            when(ugcsClient.subscribeTelemetry()).thenReturn(Flux.just(telemetry));
            when(telemetryProcessorService.process(any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(service.subscribeTelemetry())
                    .verifyComplete();

            verify(telemetryProcessorService).process(telemetry);
        }
    }

    @Nested
    @DisplayName("Subscribe mission complete tests")
    class SubscribeMissionCompleteTests {

        @Test
        @DisplayName("Should subscribe to mission complete events")
        void shouldSubscribeToMissionCompleteEvents() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete",
                    System.currentTimeMillis()
            );

            when(ugcsClient.subscribeMissionComplete()).thenReturn(Flux.just(missionComplete));
            when(missionCompleteProcessorService.process(any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            StepVerifier.create(service.subscribeMissionComplete())
                    .verifyComplete();

            verify(missionCompleteProcessorService).process(missionComplete);
        }
    }

    @Nested
    @DisplayName("Retry configuration tests")
    class RetryConfigurationTests {

        @Test
        @DisplayName("Should not retry when retry disabled")
        void shouldNotRetryWhenRetryDisabled() {
            properties.getReconnect().setEnabled(false);
            service = new UgcsConnectionService(
                    ugcsClient,
                    statusNotifier,
                    telemetryProcessorService,
                    missionCompleteProcessorService,
                    properties
            );

            when(ugcsClient.connect(anyString(), anyInt(), anyString(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("Connection failed")));

            StepVerifier.create(service.connect())
                    .verifyError(RuntimeException.class);

            verify(ugcsClient, times(1)).connect(anyString(), anyInt(), anyString(), anyString());
        }
    }
}
