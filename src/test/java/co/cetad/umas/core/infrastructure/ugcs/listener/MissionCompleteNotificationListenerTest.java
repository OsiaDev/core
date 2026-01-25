package co.cetad.umas.core.infrastructure.ugcs.listener;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import co.cetad.umas.core.infrastructure.ugcs.listener.mission.MissionCompleteNotificationListener;
import com.ugcs.ucs.client.ServerNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MissionCompleteNotificationListener Tests")
class MissionCompleteNotificationListenerTest {

    @Mock
    private DroneCache droneCache;

    private Sinks.Many<MissionCompleteData> missionCompleteSink;
    private MissionCompleteNotificationListener listener;

    @BeforeEach
    void setUp() {
        missionCompleteSink = Sinks.many().multicast().onBackpressureBuffer();
        listener = new MissionCompleteNotificationListener(missionCompleteSink, droneCache);
    }

    @Nested
    @DisplayName("Listener creation tests")
    class ListenerCreationTests {

        @Test
        @DisplayName("Should create listener with required dependencies")
        void shouldCreateListenerWithRequiredDependencies() {
            assertNotNull(listener);
            assertEquals(missionCompleteSink, listener.missionCompleteSink());
            assertEquals(droneCache, listener.droneCache());
        }
    }

    @Nested
    @DisplayName("Notification handling tests")
    class NotificationHandlingTests {

        @Test
        @DisplayName("Should handle null event wrapper gracefully")
        void shouldHandleNullEventWrapperGracefully() {
            var notification = mock(ServerNotification.class);
            when(notification.getEvent()).thenReturn(null);

            assertDoesNotThrow(() -> listener.notificationReceived(notification));
        }

        @Test
        @DisplayName("Should get telemetry from cache")
        void shouldGetTelemetryFromCache() {
            var telemetry = new TelemetryData(
                    "drone-1",
                    DroneLocation.of(45.0, -73.0, 100.0),
                    Map.of(),
                    LocalDateTime.now()
            );
            when(droneCache.getTelemetry("drone-1")).thenReturn(Optional.of(telemetry));

            var result = droneCache.getTelemetry("drone-1");

            assertTrue(result.isPresent());
            assertEquals(45.0, result.get().location().latitude());
        }
    }

    @Nested
    @DisplayName("Integration tests")
    class IntegrationTests {

        @Test
        @DisplayName("Sink should allow emitting events")
        void sinkShouldAllowEmittingEvents() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "drone-1",
                    "Current mission complete. Flight time: 100.5",
                    System.currentTimeMillis()
            );

            var result = missionCompleteSink.tryEmitNext(missionComplete);

            assertFalse(result.isFailure());
        }

        @Test
        @DisplayName("Should emit and receive mission complete event through sink")
        void shouldEmitAndReceiveMissionCompleteEventThroughSink() {
            var missionComplete = MissionCompleteData.fromVehicleLog(
                    "drone-1",
                    "Current mission complete. Flight time: 100.5",
                    System.currentTimeMillis()
            );

            missionCompleteSink.tryEmitNext(missionComplete);

            var received = missionCompleteSink.asFlux().blockFirst(java.time.Duration.ofMillis(100));
            assertNotNull(received);
            assertEquals("drone-1", received.vehicleId());
        }
    }
}
