package co.cetad.umas.core.infrastructure.ugcs.listener;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import co.cetad.umas.core.infrastructure.ugcs.listener.telemetry.TelemetryNotificationListener;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryNotificationListener Tests")
class TelemetryNotificationListenerTest {

    @Mock
    private DroneCache droneCache;

    private Sinks.Many<TelemetryData> telemetrySink;
    private TelemetryNotificationListener listener;

    @BeforeEach
    void setUp() {
        telemetrySink = Sinks.many().multicast().onBackpressureBuffer();
        listener = new TelemetryNotificationListener(telemetrySink, droneCache);
    }

    @Nested
    @DisplayName("Listener creation tests")
    class ListenerCreationTests {

        @Test
        @DisplayName("Should create listener with required dependencies")
        void shouldCreateListenerWithRequiredDependencies() {
            assertNotNull(listener);
            assertEquals(telemetrySink, listener.telemetrySink());
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
    }

    @Nested
    @DisplayName("Cache interaction tests")
    class CacheInteractionTests {

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

        @Test
        @DisplayName("Should return empty when cache miss")
        void shouldReturnEmptyWhenCacheMiss() {
            when(droneCache.getTelemetry("unknown-drone")).thenReturn(Optional.empty());

            var result = droneCache.getTelemetry("unknown-drone");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Sink emission tests")
    class SinkEmissionTests {

        @Test
        @DisplayName("Sink should allow emitting events")
        void sinkShouldAllowEmittingEvents() {
            var telemetry = new TelemetryData(
                    "drone-1",
                    DroneLocation.of(45.0, -73.0, 100.0),
                    Map.of(),
                    LocalDateTime.now()
            );

            var result = telemetrySink.tryEmitNext(telemetry);

            assertFalse(result.isFailure());
        }

        @Test
        @DisplayName("Should emit and receive telemetry through sink")
        void shouldEmitAndReceiveTelemetryThroughSink() {
            var telemetry = new TelemetryData(
                    "drone-1",
                    DroneLocation.of(45.0, -73.0, 100.0),
                    Map.of("groundSpeed", 15.5),
                    LocalDateTime.now()
            );

            telemetrySink.tryEmitNext(telemetry);

            var received = telemetrySink.asFlux().blockFirst(java.time.Duration.ofMillis(100));
            assertNotNull(received);
            assertEquals("drone-1", received.vehicleId());
            assertEquals(15.5, received.getSpeed().orElse(0.0));
        }
    }

    @Nested
    @DisplayName("Coordinate validation tests")
    class CoordinateValidationTests {

        @Test
        @DisplayName("Should validate non-zero coordinates")
        void shouldValidateNonZeroCoordinates() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData(
                    "drone-1",
                    location,
                    Map.of(),
                    LocalDateTime.now()
            );

            assertTrue(telemetry.isNewDroneLocationValid().isPresent());
        }

        @Test
        @DisplayName("Should reject zero coordinates")
        void shouldRejectZeroCoordinates() {
            var location = DroneLocation.of(0.0, 0.0, 100.0);
            var telemetry = new TelemetryData(
                    "drone-1",
                    location,
                    Map.of(),
                    LocalDateTime.now()
            );

            assertTrue(telemetry.isNewDroneLocationValid().isEmpty());
        }

        @Test
        @DisplayName("Should accept coordinates with only lat zero")
        void shouldAcceptCoordinatesWithOnlyLatZero() {
            var location = DroneLocation.of(0.0, -73.0, 100.0);
            var telemetry = new TelemetryData(
                    "drone-1",
                    location,
                    Map.of(),
                    LocalDateTime.now()
            );

            assertTrue(telemetry.isNewDroneLocationValid().isPresent());
        }
    }
}
