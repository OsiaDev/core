package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouteDefinition Tests")
class RouteDefinitionTest {

    @Nested
    @DisplayName("RouteDefinition tests")
    class RouteDefinitionMainTests {

        @Test
        @DisplayName("Should create RouteDefinition with all parameters")
        void shouldCreateRouteDefinitionWithAllParameters() {
            var waypoints = List.of(
                    new RouteDefinition.Waypoint(45.0, -73.0, 100.0, 10.0, 90.0, 5.0,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
            var settings = RouteDefinition.RouteSettings.defaults();

            var route = new RouteDefinition("route-1", "vehicle-1", waypoints, settings);

            assertEquals("route-1", route.routeName());
            assertEquals("vehicle-1", route.vehicleId());
            assertEquals(waypoints, route.waypoints());
            assertEquals(settings, route.settings());
        }
    }

    @Nested
    @DisplayName("Waypoint tests")
    class WaypointTests {

        @Test
        @DisplayName("Should create Waypoint with all parameters")
        void shouldCreateWaypointWithAllParameters() {
            var waypoint = new RouteDefinition.Waypoint(
                    45.0, -73.0, 100.0, 10.0, 90.0, 5.0,
                    RouteDefinition.Waypoint.AltitudeType.AGL
            );

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
            assertEquals(100.0, waypoint.altitude());
            assertEquals(10.0, waypoint.speed());
            assertEquals(90.0, waypoint.heading());
            assertEquals(5.0, waypoint.acceptanceRadius());
            assertEquals(RouteDefinition.Waypoint.AltitudeType.AGL, waypoint.altitudeType());
        }

        @Test
        @DisplayName("Should create Waypoint with null optional fields")
        void shouldCreateWaypointWithNullOptionalFields() {
            var waypoint = new RouteDefinition.Waypoint(
                    45.0, -73.0, 100.0, null, null, null,
                    RouteDefinition.Waypoint.AltitudeType.AGL
            );

            assertNull(waypoint.speed());
            assertNull(waypoint.heading());
            assertNull(waypoint.acceptanceRadius());
        }

        @Test
        @DisplayName("Should throw exception for invalid latitude")
        void shouldThrowExceptionForInvalidLatitude() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RouteDefinition.Waypoint(91.0, 0.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RouteDefinition.Waypoint(-91.0, 0.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }

        @Test
        @DisplayName("Should throw exception for invalid longitude")
        void shouldThrowExceptionForInvalidLongitude() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RouteDefinition.Waypoint(0.0, 181.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new RouteDefinition.Waypoint(0.0, -181.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }

        @Test
        @DisplayName("Should throw exception for negative altitude")
        void shouldThrowExceptionForNegativeAltitude() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RouteDefinition.Waypoint(45.0, -73.0, -1.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }

        @Test
        @DisplayName("Should accept boundary latitude values")
        void shouldAcceptBoundaryLatitudeValues() {
            assertDoesNotThrow(() ->
                    new RouteDefinition.Waypoint(90.0, 0.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
            assertDoesNotThrow(() ->
                    new RouteDefinition.Waypoint(-90.0, 0.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }

        @Test
        @DisplayName("Should accept boundary longitude values")
        void shouldAcceptBoundaryLongitudeValues() {
            assertDoesNotThrow(() ->
                    new RouteDefinition.Waypoint(0.0, 180.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
            assertDoesNotThrow(() ->
                    new RouteDefinition.Waypoint(0.0, -180.0, 100.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }

        @Test
        @DisplayName("Should accept zero altitude")
        void shouldAcceptZeroAltitude() {
            assertDoesNotThrow(() ->
                    new RouteDefinition.Waypoint(45.0, -73.0, 0.0, null, null, null,
                            RouteDefinition.Waypoint.AltitudeType.AGL)
            );
        }
    }

    @Nested
    @DisplayName("AltitudeType tests")
    class AltitudeTypeTests {

        @Test
        @DisplayName("Should have all expected altitude types")
        void shouldHaveAllExpectedAltitudeTypes() {
            var types = RouteDefinition.Waypoint.AltitudeType.values();

            assertEquals(3, types.length);
            assertNotNull(RouteDefinition.Waypoint.AltitudeType.AGL);
            assertNotNull(RouteDefinition.Waypoint.AltitudeType.AMSL);
            assertNotNull(RouteDefinition.Waypoint.AltitudeType.WGS84);
        }
    }

    @Nested
    @DisplayName("RouteSettings tests")
    class RouteSettingsTests {

        @Test
        @DisplayName("Should create RouteSettings with all parameters")
        void shouldCreateRouteSettingsWithAllParameters() {
            var settings = new RouteDefinition.RouteSettings(
                    10.0, 100.0, 5.0, true
            );

            assertEquals(10.0, settings.defaultSpeed());
            assertEquals(100.0, settings.defaultAltitude());
            assertEquals(5.0, settings.defaultAcceptanceRadius());
            assertTrue(settings.autoStart());
        }

        @Test
        @DisplayName("Should create default RouteSettings")
        void shouldCreateDefaultRouteSettings() {
            var settings = RouteDefinition.RouteSettings.defaults();

            assertEquals(5.0, settings.defaultSpeed());
            assertEquals(50.0, settings.defaultAltitude());
            assertEquals(5.0, settings.defaultAcceptanceRadius());
            assertFalse(settings.autoStart());
        }
    }
}
