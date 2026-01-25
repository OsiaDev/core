package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelemetryEvent Tests")
class TelemetryEventTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create TelemetryEvent with all parameters")
        void shouldCreateTelemetryEventWithAllParameters() {
            var timestamp = LocalDateTime.now();
            var additionalFields = Map.<String, Object>of("customField", "value");

            var event = new TelemetryEvent(
                    "vehicle-1",
                    45.0,
                    -73.0,
                    100.0,
                    15.5,
                    270.0,
                    85.5,
                    12,
                    timestamp,
                    additionalFields
            );

            assertEquals("vehicle-1", event.vehicleId());
            assertEquals(45.0, event.latitude());
            assertEquals(-73.0, event.longitude());
            assertEquals(100.0, event.altitude());
            assertEquals(15.5, event.speed());
            assertEquals(270.0, event.heading());
            assertEquals(85.5, event.batteryLevel());
            assertEquals(12, event.satelliteCount());
            assertEquals(timestamp, event.timestamp());
            assertEquals(additionalFields, event.additionalFields());
        }

        @Test
        @DisplayName("Should create TelemetryEvent with null optional fields")
        void shouldCreateTelemetryEventWithNullOptionalFields() {
            var timestamp = LocalDateTime.now();

            var event = new TelemetryEvent(
                    "vehicle-1",
                    45.0,
                    -73.0,
                    100.0,
                    null,
                    null,
                    null,
                    null,
                    timestamp,
                    Map.of()
            );

            assertEquals("vehicle-1", event.vehicleId());
            assertNull(event.speed());
            assertNull(event.heading());
            assertNull(event.batteryLevel());
            assertNull(event.satelliteCount());
        }
    }

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create TelemetryEvent from TelemetryData")
        void shouldCreateTelemetryEventFromTelemetryData() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of(
                    "groundSpeed", 15.5,
                    "heading", 270.0,
                    "batteryLevel", 85.5,
                    "satelliteCount", 12
            );
            var timestamp = LocalDateTime.now();
            var telemetryData = new TelemetryData("vehicle-1", location, fields, timestamp);

            var event = TelemetryEvent.from(telemetryData);

            assertEquals("vehicle-1", event.vehicleId());
            assertEquals(45.0, event.latitude());
            assertEquals(-73.0, event.longitude());
            assertEquals(100.0, event.altitude());
            assertEquals(15.5, event.speed());
            assertEquals(270.0, event.heading());
            assertEquals(85.5, event.batteryLevel());
            assertEquals(12, event.satelliteCount());
            assertEquals(timestamp, event.timestamp());
            assertEquals(fields, event.additionalFields());
        }

        @Test
        @DisplayName("Should create TelemetryEvent from TelemetryData with missing optional fields")
        void shouldCreateTelemetryEventFromTelemetryDataWithMissingOptionalFields() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var timestamp = LocalDateTime.now();
            var telemetryData = new TelemetryData("vehicle-1", location, Map.of(), timestamp);

            var event = TelemetryEvent.from(telemetryData);

            assertEquals("vehicle-1", event.vehicleId());
            assertEquals(45.0, event.latitude());
            assertEquals(-73.0, event.longitude());
            assertEquals(100.0, event.altitude());
            assertNull(event.speed());
            assertNull(event.heading());
            assertNull(event.batteryLevel());
            assertNull(event.satelliteCount());
        }
    }
}
