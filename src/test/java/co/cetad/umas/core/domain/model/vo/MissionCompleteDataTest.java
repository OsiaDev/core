package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MissionCompleteData Tests")
class MissionCompleteDataTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create MissionCompleteData with all parameters")
        void shouldCreateMissionCompleteDataWithAllParameters() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var timestamp = Instant.now();

            var data = new MissionCompleteData(
                    "vehicle-1",
                    location,
                    123.45,
                    "Mission completed",
                    "mission-1",
                    timestamp
            );

            assertEquals("vehicle-1", data.vehicleId());
            assertEquals(location, data.location());
            assertEquals(123.45, data.flightTimeSeconds());
            assertEquals("Mission completed", data.message());
            assertEquals("mission-1", data.missionId());
            assertEquals(timestamp, data.timestamp());
        }
    }

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create from vehicle log")
        void shouldCreateFromVehicleLog() {
            long timeMillis = System.currentTimeMillis();

            var data = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: 93.46499991416931",
                    timeMillis
            );

            assertEquals("vehicle-1", data.vehicleId());
            assertNull(data.location());
            assertEquals(93.46499991416931, data.flightTimeSeconds());
            assertNull(data.missionId());
            assertEquals(Instant.ofEpochMilli(timeMillis), data.timestamp());
        }

        @Test
        @DisplayName("Should create from vehicle log without flight time")
        void shouldCreateFromVehicleLogWithoutFlightTime() {
            var data = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete.",
                    System.currentTimeMillis()
            );

            assertNull(data.flightTimeSeconds());
        }

        @Test
        @DisplayName("Should create from vehicle log with location")
        void shouldCreateFromVehicleLogWithLocation() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            long timeMillis = System.currentTimeMillis();

            var data = MissionCompleteData.fromVehicleLogWithLocation(
                    "vehicle-1",
                    location,
                    "Current mission complete. Flight time: 100.5",
                    timeMillis
            );

            assertEquals("vehicle-1", data.vehicleId());
            assertEquals(location, data.location());
            assertEquals(100.5, data.flightTimeSeconds());
        }

        @Test
        @DisplayName("Should handle null message when extracting flight time")
        void shouldHandleNullMessageWhenExtractingFlightTime() {
            var data = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    null,
                    System.currentTimeMillis()
            );

            assertNull(data.flightTimeSeconds());
        }

        @Test
        @DisplayName("Should handle invalid flight time format")
        void shouldHandleInvalidFlightTimeFormat() {
            var data = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: invalid",
                    System.currentTimeMillis()
            );

            assertNull(data.flightTimeSeconds());
        }
    }

    @Nested
    @DisplayName("With methods tests")
    class WithMethodsTests {

        @Test
        @DisplayName("Should create copy with missionId")
        void shouldCreateCopyWithMissionId() {
            var original = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: 100.0",
                    System.currentTimeMillis()
            );

            var updated = original.withMissionId("mission-123");

            assertEquals("mission-123", updated.missionId());
            assertEquals(original.vehicleId(), updated.vehicleId());
            assertEquals(original.flightTimeSeconds(), updated.flightTimeSeconds());
            assertEquals(original.message(), updated.message());
            assertEquals(original.timestamp(), updated.timestamp());
        }

        @Test
        @DisplayName("Should create copy with location")
        void shouldCreateCopyWithLocation() {
            var original = MissionCompleteData.fromVehicleLog(
                    "vehicle-1",
                    "Current mission complete. Flight time: 100.0",
                    System.currentTimeMillis()
            );
            var location = DroneLocation.of(45.0, -73.0, 100.0);

            var updated = original.withLocation(location);

            assertEquals(location, updated.location());
            assertEquals(original.vehicleId(), updated.vehicleId());
            assertEquals(original.flightTimeSeconds(), updated.flightTimeSeconds());
            assertEquals(original.message(), updated.message());
            assertEquals(original.timestamp(), updated.timestamp());
        }
    }
}
