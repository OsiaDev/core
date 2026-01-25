package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TelemetryData Tests")
class TelemetryDataTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create TelemetryData with all parameters")
        void shouldCreateTelemetryDataWithAllParameters() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("groundSpeed", 10.5, "batteryLevel", 85.0);
            var timestamp = LocalDateTime.now();

            var telemetry = new TelemetryData("vehicle-1", location, fields, timestamp);

            assertEquals("vehicle-1", telemetry.vehicleId());
            assertEquals(location, telemetry.location());
            assertEquals(fields, telemetry.fields());
            assertEquals(timestamp, telemetry.timestamp());
        }
    }

    @Nested
    @DisplayName("getSpeed tests")
    class GetSpeedTests {

        @Test
        @DisplayName("Should return speed when present")
        void shouldReturnSpeedWhenPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("groundSpeed", 15.5);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var speed = telemetry.getSpeed();

            assertTrue(speed.isPresent());
            assertEquals(15.5, speed.get());
        }

        @Test
        @DisplayName("Should return empty when speed not present")
        void shouldReturnEmptyWhenSpeedNotPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var speed = telemetry.getSpeed();

            assertTrue(speed.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when speed is not a number")
        void shouldReturnEmptyWhenSpeedIsNotANumber() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("groundSpeed", "fast");
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var speed = telemetry.getSpeed();

            assertTrue(speed.isEmpty());
        }

        @Test
        @DisplayName("Should handle integer speed value")
        void shouldHandleIntegerSpeedValue() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("groundSpeed", 15);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var speed = telemetry.getSpeed();

            assertTrue(speed.isPresent());
            assertEquals(15.0, speed.get());
        }
    }

    @Nested
    @DisplayName("getBatteryLevel tests")
    class GetBatteryLevelTests {

        @Test
        @DisplayName("Should return battery level when present")
        void shouldReturnBatteryLevelWhenPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("batteryLevel", 85.5);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var batteryLevel = telemetry.getBatteryLevel();

            assertTrue(batteryLevel.isPresent());
            assertEquals(85.5, batteryLevel.get());
        }

        @Test
        @DisplayName("Should return empty when battery level not present")
        void shouldReturnEmptyWhenBatteryLevelNotPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var batteryLevel = telemetry.getBatteryLevel();

            assertTrue(batteryLevel.isEmpty());
        }
    }

    @Nested
    @DisplayName("getHeading tests")
    class GetHeadingTests {

        @Test
        @DisplayName("Should return heading when present")
        void shouldReturnHeadingWhenPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("heading", 270.0);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var heading = telemetry.getHeading();

            assertTrue(heading.isPresent());
            assertEquals(270.0, heading.get());
        }

        @Test
        @DisplayName("Should return empty when heading not present")
        void shouldReturnEmptyWhenHeadingNotPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var heading = telemetry.getHeading();

            assertTrue(heading.isEmpty());
        }
    }

    @Nested
    @DisplayName("getSatelliteCount tests")
    class GetSatelliteCountTests {

        @Test
        @DisplayName("Should return satellite count when present")
        void shouldReturnSatelliteCountWhenPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("satelliteCount", 12);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var satelliteCount = telemetry.getSatelliteCount();

            assertTrue(satelliteCount.isPresent());
            assertEquals(12, satelliteCount.get());
        }

        @Test
        @DisplayName("Should return empty when satellite count not present")
        void shouldReturnEmptyWhenSatelliteCountNotPresent() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var satelliteCount = telemetry.getSatelliteCount();

            assertTrue(satelliteCount.isEmpty());
        }

        @Test
        @DisplayName("Should handle double satellite count value")
        void shouldHandleDoubleSatelliteCountValue() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of("satelliteCount", 12.7);
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            var satelliteCount = telemetry.getSatelliteCount();

            assertTrue(satelliteCount.isPresent());
            assertEquals(12, satelliteCount.get());
        }
    }

    @Nested
    @DisplayName("isNewDroneLocationValid tests")
    class IsNewDroneLocationValidTests {

        @Test
        @DisplayName("Should return location when valid")
        void shouldReturnLocationWhenValid() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var validLocation = telemetry.isNewDroneLocationValid();

            assertTrue(validLocation.isPresent());
            assertEquals(location, validLocation.get());
        }

        @Test
        @DisplayName("Should return empty when location is null")
        void shouldReturnEmptyWhenLocationIsNull() {
            var telemetry = new TelemetryData("vehicle-1", null, Map.of(), LocalDateTime.now());

            var validLocation = telemetry.isNewDroneLocationValid();

            assertTrue(validLocation.isEmpty());
        }

        @Test
        @DisplayName("Should return empty when lat and lon are zero")
        void shouldReturnEmptyWhenLatAndLonAreZero() {
            var location = DroneLocation.of(0.0, 0.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var validLocation = telemetry.isNewDroneLocationValid();

            assertTrue(validLocation.isEmpty());
        }

        @Test
        @DisplayName("Should return location when only lat is zero")
        void shouldReturnLocationWhenOnlyLatIsZero() {
            var location = DroneLocation.of(0.0, -73.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var validLocation = telemetry.isNewDroneLocationValid();

            assertTrue(validLocation.isPresent());
        }

        @Test
        @DisplayName("Should return location when only lon is zero")
        void shouldReturnLocationWhenOnlyLonIsZero() {
            var location = DroneLocation.of(45.0, 0.0, 100.0);
            var telemetry = new TelemetryData("vehicle-1", location, Map.of(), LocalDateTime.now());

            var validLocation = telemetry.isNewDroneLocationValid();

            assertTrue(validLocation.isPresent());
        }
    }
}
