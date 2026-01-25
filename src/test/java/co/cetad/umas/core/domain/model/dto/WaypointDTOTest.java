package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WaypointDTO Tests")
class WaypointDTOTest {

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should create valid WaypointDTO")
        void shouldCreateValidWaypointDTO() {
            var waypoint = new WaypointDTO(45.0, -73.0);

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
        }

        @Test
        @DisplayName("Should accept boundary latitude values")
        void shouldAcceptBoundaryLatitudeValues() {
            assertDoesNotThrow(() -> new WaypointDTO(90.0, 0.0));
            assertDoesNotThrow(() -> new WaypointDTO(-90.0, 0.0));
        }

        @Test
        @DisplayName("Should accept boundary longitude values")
        void shouldAcceptBoundaryLongitudeValues() {
            assertDoesNotThrow(() -> new WaypointDTO(0.0, 180.0));
            assertDoesNotThrow(() -> new WaypointDTO(0.0, -180.0));
        }

        @Test
        @DisplayName("Should throw exception for latitude out of range")
        void shouldThrowExceptionForLatitudeOutOfRange() {
            assertThrows(IllegalArgumentException.class, () ->
                    new WaypointDTO(90.1, 0.0)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new WaypointDTO(-90.1, 0.0)
            );
        }

        @Test
        @DisplayName("Should throw exception for longitude out of range")
        void shouldThrowExceptionForLongitudeOutOfRange() {
            assertThrows(IllegalArgumentException.class, () ->
                    new WaypointDTO(0.0, 180.1)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new WaypointDTO(0.0, -180.1)
            );
        }
    }

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create from GeoJSON coordinates")
        void shouldCreateFromGeoJsonCoordinates() {
            // GeoJSON uses [longitude, latitude] format
            double[] coords = {-73.0, 45.0};

            var waypoint = WaypointDTO.fromGeoJsonCoordinates(coords);

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
        }

        @Test
        @DisplayName("Should throw exception for null coordinates")
        void shouldThrowExceptionForNullCoordinates() {
            assertThrows(IllegalArgumentException.class, () ->
                    WaypointDTO.fromGeoJsonCoordinates(null)
            );
        }

        @Test
        @DisplayName("Should throw exception for insufficient coordinates")
        void shouldThrowExceptionForInsufficientCoordinates() {
            assertThrows(IllegalArgumentException.class, () ->
                    WaypointDTO.fromGeoJsonCoordinates(new double[]{0.0})
            );
        }

        @Test
        @DisplayName("Should accept coordinates array with more than 2 elements")
        void shouldAcceptCoordinatesArrayWithMoreThan2Elements() {
            double[] coords = {-73.0, 45.0, 100.0}; // longitude, latitude, altitude

            var waypoint = WaypointDTO.fromGeoJsonCoordinates(coords);

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
        }
    }
}
