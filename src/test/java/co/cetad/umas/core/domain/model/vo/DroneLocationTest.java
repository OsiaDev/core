package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DroneLocation Tests")
class DroneLocationTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create DroneLocation with all parameters")
        void shouldCreateDroneLocationWithAllParameters() {
            var timestamp = LocalDateTime.now();
            var location = new DroneLocation(45.0, -73.0, 100.0, timestamp);

            assertEquals(45.0, location.latitude());
            assertEquals(-73.0, location.longitude());
            assertEquals(100.0, location.altitude());
            assertEquals(timestamp, location.timestamp());
        }
    }

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create DroneLocation using factory method")
        void shouldCreateDroneLocationUsingFactoryMethod() {
            var location = DroneLocation.of(45.0, -73.0, 100.0);

            assertEquals(45.0, location.latitude());
            assertEquals(-73.0, location.longitude());
            assertEquals(100.0, location.altitude());
            assertNotNull(location.timestamp());
        }

        @Test
        @DisplayName("Factory method should set current timestamp")
        void factoryMethodShouldSetCurrentTimestamp() {
            var before = LocalDateTime.now();
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var after = LocalDateTime.now();

            assertFalse(location.timestamp().isBefore(before));
            assertFalse(location.timestamp().isAfter(after));
        }
    }

    @Nested
    @DisplayName("Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle zero coordinates")
        void shouldHandleZeroCoordinates() {
            var location = DroneLocation.of(0.0, 0.0, 0.0);

            assertEquals(0.0, location.latitude());
            assertEquals(0.0, location.longitude());
            assertEquals(0.0, location.altitude());
        }

        @Test
        @DisplayName("Should handle negative altitude")
        void shouldHandleNegativeAltitude() {
            var location = DroneLocation.of(45.0, -73.0, -10.0);

            assertEquals(-10.0, location.altitude());
        }

        @Test
        @DisplayName("Should handle boundary latitude values")
        void shouldHandleBoundaryLatitudeValues() {
            var location1 = DroneLocation.of(90.0, 0.0, 0.0);
            var location2 = DroneLocation.of(-90.0, 0.0, 0.0);

            assertEquals(90.0, location1.latitude());
            assertEquals(-90.0, location2.latitude());
        }

        @Test
        @DisplayName("Should handle boundary longitude values")
        void shouldHandleBoundaryLongitudeValues() {
            var location1 = DroneLocation.of(0.0, 180.0, 0.0);
            var location2 = DroneLocation.of(0.0, -180.0, 0.0);

            assertEquals(180.0, location1.longitude());
            assertEquals(-180.0, location2.longitude());
        }
    }
}
