package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouteExecutionDTO Tests")
class RouteExecutionDTOTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create RouteExecutionDTO with all parameters")
        void shouldCreateRouteExecutionDTOWithAllParameters() {
            var waypoints = List.of(
                    new WaypointDTO(45.0, -73.0),
                    new WaypointDTO(46.0, -74.0)
            );

            var dto = new RouteExecutionDTO(
                    "vehicle-1",
                    "mission-1",
                    "route-1",
                    "route-id-123",
                    waypoints,
                    1
            );

            assertEquals("vehicle-1", dto.vehicleId());
            assertEquals("mission-1", dto.missionId());
            assertEquals("route-1", dto.routeName());
            assertEquals("route-id-123", dto.routeId());
            assertEquals(2, dto.waypoints().size());
            assertEquals(1, dto.priority());
        }

        @Test
        @DisplayName("Should create RouteExecutionDTO with null optional fields")
        void shouldCreateRouteExecutionDTOWithNullOptionalFields() {
            var dto = new RouteExecutionDTO(
                    "vehicle-1",
                    null,
                    null,
                    null,
                    null,
                    null
            );

            assertEquals("vehicle-1", dto.vehicleId());
            assertNull(dto.missionId());
            assertNull(dto.routeName());
            assertNull(dto.routeId());
            assertNull(dto.waypoints());
            assertNull(dto.priority());
        }

        @Test
        @DisplayName("Should create RouteExecutionDTO with empty waypoints")
        void shouldCreateRouteExecutionDTOWithEmptyWaypoints() {
            var dto = new RouteExecutionDTO(
                    "vehicle-1",
                    "mission-1",
                    "route-1",
                    "route-id-123",
                    List.of(),
                    1
            );

            assertTrue(dto.waypoints().isEmpty());
        }
    }
}
