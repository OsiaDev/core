package co.cetad.umas.core.domain.model.dto;

import co.cetad.umas.core.domain.model.vo.RouteDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouteUploadDTO Tests")
class RouteUploadDTOTest {

    @Nested
    @DisplayName("WaypointDTO tests")
    class WaypointDTOInnerTests {

        @Test
        @DisplayName("Should create WaypointDTO with all parameters")
        void shouldCreateWaypointDTOWithAllParameters() {
            var waypoint = new RouteUploadDTO.WaypointDTO(
                    45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "AGL"
            );

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
            assertEquals(100.0, waypoint.altitude());
            assertEquals(10.0, waypoint.speed());
            assertEquals(90.0, waypoint.heading());
            assertEquals(5.0, waypoint.acceptanceRadius());
            assertEquals("AGL", waypoint.altitudeType());
        }

        @Test
        @DisplayName("Should convert to domain with AGL altitude type")
        void shouldConvertToDomainWithAGLAltitudeType() {
            var waypoint = new RouteUploadDTO.WaypointDTO(
                    45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "AGL"
            );

            var domain = waypoint.toDomain();

            assertEquals(45.0, domain.latitude());
            assertEquals(-73.0, domain.longitude());
            assertEquals(100.0, domain.altitude());
            assertEquals(RouteDefinition.Waypoint.AltitudeType.AGL, domain.altitudeType());
        }

        @Test
        @DisplayName("Should convert to domain with AMSL altitude type")
        void shouldConvertToDomainWithAMSLAltitudeType() {
            var waypoint = new RouteUploadDTO.WaypointDTO(
                    45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "AMSL"
            );

            var domain = waypoint.toDomain();

            assertEquals(RouteDefinition.Waypoint.AltitudeType.AMSL, domain.altitudeType());
        }

        @Test
        @DisplayName("Should convert to domain with default AGL when altitudeType is null")
        void shouldConvertToDomainWithDefaultAGLWhenAltitudeTypeIsNull() {
            var waypoint = new RouteUploadDTO.WaypointDTO(
                    45.0, -73.0, 100.0, null, null, null, null
            );

            var domain = waypoint.toDomain();

            assertEquals(RouteDefinition.Waypoint.AltitudeType.AGL, domain.altitudeType());
        }

        @Test
        @DisplayName("Should convert to domain with lowercase altitude type")
        void shouldConvertToDomainWithLowercaseAltitudeType() {
            var waypoint = new RouteUploadDTO.WaypointDTO(
                    45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "wgs84"
            );

            var domain = waypoint.toDomain();

            assertEquals(RouteDefinition.Waypoint.AltitudeType.WGS84, domain.altitudeType());
        }
    }

    @Nested
    @DisplayName("RouteSettingsDTO tests")
    class RouteSettingsDTOTests {

        @Test
        @DisplayName("Should create RouteSettingsDTO with all parameters")
        void shouldCreateRouteSettingsDTOWithAllParameters() {
            var settings = new RouteUploadDTO.RouteSettingsDTO(
                    10.0, 100.0, 5.0, true
            );

            assertEquals(10.0, settings.defaultSpeed());
            assertEquals(100.0, settings.defaultAltitude());
            assertEquals(5.0, settings.defaultAcceptanceRadius());
            assertTrue(settings.autoStart());
        }

        @Test
        @DisplayName("Should convert to domain with all values")
        void shouldConvertToDomainWithAllValues() {
            var settings = new RouteUploadDTO.RouteSettingsDTO(
                    10.0, 100.0, 5.0, true
            );

            var domain = settings.toDomain();

            assertEquals(10.0, domain.defaultSpeed());
            assertEquals(100.0, domain.defaultAltitude());
            assertEquals(5.0, domain.defaultAcceptanceRadius());
            assertTrue(domain.autoStart());
        }

        @Test
        @DisplayName("Should convert to domain with default values when null")
        void shouldConvertToDomainWithDefaultValuesWhenNull() {
            var settings = new RouteUploadDTO.RouteSettingsDTO(
                    null, null, null, null
            );

            var domain = settings.toDomain();

            assertEquals(5.0, domain.defaultSpeed());
            assertEquals(50.0, domain.defaultAltitude());
            assertEquals(5.0, domain.defaultAcceptanceRadius());
            assertFalse(domain.autoStart());
        }
    }

    @Nested
    @DisplayName("RouteUploadDTO toDomain tests")
    class RouteUploadDTOToDomainTests {

        @Test
        @DisplayName("Should convert to domain with settings")
        void shouldConvertToDomainWithSettings() {
            var waypoints = List.of(
                    new RouteUploadDTO.WaypointDTO(45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "AGL")
            );
            var settings = new RouteUploadDTO.RouteSettingsDTO(10.0, 100.0, 5.0, true);
            var dto = new RouteUploadDTO("vehicle-1", "route-1", waypoints, settings, 1);

            var domain = dto.toDomain();

            assertEquals("route-1", domain.routeName());
            assertEquals("vehicle-1", domain.vehicleId());
            assertEquals(1, domain.waypoints().size());
            assertEquals(10.0, domain.settings().defaultSpeed());
        }

        @Test
        @DisplayName("Should convert to domain with default settings when null")
        void shouldConvertToDomainWithDefaultSettingsWhenNull() {
            var waypoints = List.of(
                    new RouteUploadDTO.WaypointDTO(45.0, -73.0, 100.0, 10.0, 90.0, 5.0, "AGL")
            );
            var dto = new RouteUploadDTO("vehicle-1", "route-1", waypoints, null, 1);

            var domain = dto.toDomain();

            assertEquals(5.0, domain.settings().defaultSpeed());
            assertEquals(50.0, domain.settings().defaultAltitude());
            assertFalse(domain.settings().autoStart());
        }
    }
}
