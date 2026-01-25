package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MissionExecutionDTO Tests")
class MissionExecutionDTOTest {

    @Nested
    @DisplayName("MissionExecutionDTO validation tests")
    class MissionExecutionDTOValidationTests {

        @Test
        @DisplayName("Should create valid MissionExecutionDTO")
        void shouldCreateValidMissionExecutionDTO() {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(10.0, 20.0),
                    new MissionExecutionDTO.SimpleWaypoint(11.0, 21.0)
            );
            var drones = List.of(
                    new MissionExecutionDTO.DroneExecution("drone-1", "route-1", 50.0, 100.0, waypoints)
            );

            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            assertEquals("mission-1", mission.missionId());
            assertEquals(1, mission.drones().size());
            assertEquals(1, mission.priority());
        }

        @Test
        @DisplayName("Should throw exception when drones is null")
        void shouldThrowExceptionWhenDronesIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new MissionExecutionDTO("mission-1", null, 1)
            );
        }

        @Test
        @DisplayName("Should throw exception when missionId is null")
        void shouldThrowExceptionWhenMissionIdIsNull() {
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1")
            );

            assertThrows(NullPointerException.class, () ->
                    new MissionExecutionDTO(null, drones, 1)
            );
        }

        @Test
        @DisplayName("Should throw exception when drone list is empty")
        void shouldThrowExceptionWhenDroneListIsEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO("mission-1", List.of(), 1)
            );
        }

        @Test
        @DisplayName("Should throw exception when missionId is blank")
        void shouldThrowExceptionWhenMissionIdIsBlank() {
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO("  ", drones, 1)
            );
        }

        @Test
        @DisplayName("Should set default priority when null or negative")
        void shouldSetDefaultPriorityWhenNullOrNegative() {
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1")
            );

            var mission1 = new MissionExecutionDTO("mission-1", drones, null);
            var mission2 = new MissionExecutionDTO("mission-1", drones, -5);

            assertEquals(1, mission1.priority());
            assertEquals(1, mission2.priority());
        }
    }

    @Nested
    @DisplayName("SimpleWaypoint tests")
    class SimpleWaypointTests {

        @Test
        @DisplayName("Should create valid SimpleWaypoint")
        void shouldCreateValidSimpleWaypoint() {
            var waypoint = new MissionExecutionDTO.SimpleWaypoint(45.0, -73.0);

            assertEquals(45.0, waypoint.latitude());
            assertEquals(-73.0, waypoint.longitude());
        }

        @Test
        @DisplayName("Should throw exception for invalid latitude")
        void shouldThrowExceptionForInvalidLatitude() {
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO.SimpleWaypoint(91.0, 0.0)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO.SimpleWaypoint(-91.0, 0.0)
            );
        }

        @Test
        @DisplayName("Should throw exception for invalid longitude")
        void shouldThrowExceptionForInvalidLongitude() {
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO.SimpleWaypoint(0.0, 181.0)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO.SimpleWaypoint(0.0, -181.0)
            );
        }

        @Test
        @DisplayName("Should convert latitude to radians")
        void shouldConvertLatitudeToRadians() {
            var waypoint = new MissionExecutionDTO.SimpleWaypoint(90.0, 0.0);
            assertEquals(Math.PI / 2, waypoint.latitudeRadians(), 0.0001);
        }

        @Test
        @DisplayName("Should convert longitude to radians")
        void shouldConvertLongitudeToRadians() {
            var waypoint = new MissionExecutionDTO.SimpleWaypoint(0.0, 180.0);
            assertEquals(Math.PI, waypoint.longitudeRadians(), 0.0001);
        }
    }

    @Nested
    @DisplayName("DroneExecution tests")
    class DroneExecutionTests {

        @Test
        @DisplayName("Should create valid DroneExecution")
        void shouldCreateValidDroneExecution() {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(10.0, 20.0)
            );
            var drone = new MissionExecutionDTO.DroneExecution(
                    "drone-1", "route-1", 50.0, 100.0, waypoints
            );

            assertEquals("drone-1", drone.vehicleId());
            assertEquals("route-1", drone.routeId());
            assertEquals(50.0, drone.safeAltitude());
            assertEquals(100.0, drone.maxAltitude());
            assertTrue(drone.hasWaypoints());
            assertTrue(drone.hasRouteId());
        }

        @Test
        @DisplayName("Should throw exception when vehicleId is null")
        void shouldThrowExceptionWhenVehicleIdIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new MissionExecutionDTO.DroneExecution(null, "route-1", 50.0, 100.0, List.of())
            );
        }

        @Test
        @DisplayName("Should throw exception when vehicleId is blank")
        void shouldThrowExceptionWhenVehicleIdIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new MissionExecutionDTO.DroneExecution("  ", "route-1", 50.0, 100.0, List.of())
            );
        }

        @Test
        @DisplayName("Should throw exception when waypoints is null")
        void shouldThrowExceptionWhenWaypointsIsNull() {
            assertThrows(NullPointerException.class, () ->
                    new MissionExecutionDTO.DroneExecution("drone-1", "route-1", 50.0, 100.0, null)
            );
        }

        @Test
        @DisplayName("Should create DroneExecution without route using factory method")
        void shouldCreateDroneExecutionWithoutRouteUsingFactoryMethod() {
            var drone = MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1");

            assertEquals("drone-1", drone.vehicleId());
            assertNull(drone.routeId());
            assertFalse(drone.hasWaypoints());
            assertFalse(drone.hasRouteId());
        }

        @Test
        @DisplayName("Should create DroneExecution using factory method")
        void shouldCreateDroneExecutionUsingFactoryMethod() {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(10.0, 20.0)
            );
            var drone = MissionExecutionDTO.DroneExecution.create(
                    "drone-1", "route-1", 50.0, 100.0, waypoints
            );

            assertEquals("drone-1", drone.vehicleId());
            assertTrue(drone.hasWaypoints());
        }

        @Test
        @DisplayName("hasWaypoints should return false for empty list")
        void hasWaypointsShouldReturnFalseForEmptyList() {
            var drone = new MissionExecutionDTO.DroneExecution(
                    "drone-1", "route-1", 50.0, 100.0, List.of()
            );

            assertFalse(drone.hasWaypoints());
        }

        @Test
        @DisplayName("hasRouteId should return false for blank routeId")
        void hasRouteIdShouldReturnFalseForBlankRouteId() {
            var drone = new MissionExecutionDTO.DroneExecution(
                    "drone-1", "  ", 50.0, 100.0, List.of()
            );

            assertFalse(drone.hasRouteId());
        }
    }
}
