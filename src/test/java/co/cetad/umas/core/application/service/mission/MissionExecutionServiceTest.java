package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import com.ugcs.ucs.proto.DomainProto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MissionExecutionService Tests")
class MissionExecutionServiceTest {

    @Mock
    private UgcsClient ugcsClient;

    private MissionExecutionService service;

    @BeforeEach
    void setUp() {
        service = new MissionExecutionService(ugcsClient);
    }

    @Nested
    @DisplayName("Successful mission execution tests")
    class SuccessfulMissionExecutionTests {

        @Test
        @DisplayName("Should execute mission successfully with single drone")
        void shouldExecuteMissionSuccessfullyWithSingleDrone() throws Exception {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(45.0, -73.0)
            );
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.create("drone-1", "route-1", 50.0, 100.0, waypoints)
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            var mockMission = DomainProto.Mission.newBuilder().setName("mission-1").build();
            var mockVehicle = DomainProto.Vehicle.newBuilder().setName("drone-1").build();
            var mockRoute = DomainProto.Route.newBuilder().setName("route-1").build();

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(mockMission));
            when(ugcsClient.findRouteByName(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(mockRoute)));
            when(ugcsClient.uploadExistingRoute(anyString(), any(DomainProto.Route.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockVehicle));
            when(ugcsClient.createMissionVehicle(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));

            var result = service.process(mission).get();

            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
            assertTrue(result.message().contains("successfully"));
        }

        @Test
        @DisplayName("Should execute mission with drone without waypoints")
        void shouldExecuteMissionWithDroneWithoutWaypoints() throws Exception {
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1")
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            var mockMission = DomainProto.Mission.newBuilder().setName("mission-1").build();

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(mockMission));

            var result = service.process(mission).get();

            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
        }
    }

    @Nested
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return failed when mission creation fails")
        void shouldReturnFailedWhenMissionCreationFails() throws Exception {
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.createWithoutRoute("drone-1")
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Mission error")));

            var result = service.process(mission).get();

            assertEquals(CommandResultDTO.CommandStatus.FAILED, result.status());
        }

        @Test
        @DisplayName("Should handle vehicle registration failure")
        void shouldHandleVehicleRegistrationFailure() throws Exception {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(45.0, -73.0)
            );
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.create("drone-1", "route-1", 50.0, 100.0, waypoints)
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            var mockMission = DomainProto.Mission.newBuilder().setName("mission-1").build();
            var mockVehicle = DomainProto.Vehicle.newBuilder().setName("drone-1").build();
            var mockRoute = DomainProto.Route.newBuilder().setName("route-1").build();

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(mockMission));
            when(ugcsClient.findRouteByName(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(mockRoute)));
            when(ugcsClient.uploadExistingRoute(anyString(), any(DomainProto.Route.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockVehicle));
            when(ugcsClient.createMissionVehicle(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(false));

            var result = service.process(mission).get();

            // Mission still succeeds as all drones were processed
            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
        }

        @Test
        @DisplayName("Should handle AUTO command failure")
        void shouldHandleAutoCommandFailure() throws Exception {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(45.0, -73.0)
            );
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.create("drone-1", "route-1", 50.0, 100.0, waypoints)
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            var mockMission = DomainProto.Mission.newBuilder().setName("mission-1").build();
            var mockVehicle = DomainProto.Vehicle.newBuilder().setName("drone-1").build();
            var mockRoute = DomainProto.Route.newBuilder().setName("route-1").build();

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(mockMission));
            when(ugcsClient.findRouteByName(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(mockRoute)));
            when(ugcsClient.uploadExistingRoute(anyString(), any(DomainProto.Route.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockVehicle));
            when(ugcsClient.createMissionVehicle(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(false));

            var result = service.process(mission).get();

            assertEquals(CommandResultDTO.CommandStatus.FAILED, result.status());
        }
    }

    @Nested
    @DisplayName("Multiple drones tests")
    class MultipleDronesTests {

        @Test
        @DisplayName("Should process multiple drones in parallel")
        void shouldProcessMultipleDronesInParallel() throws Exception {
            var waypoints = List.of(
                    new MissionExecutionDTO.SimpleWaypoint(45.0, -73.0)
            );
            var drones = List.of(
                    MissionExecutionDTO.DroneExecution.create("drone-1", "route-1", 50.0, 100.0, waypoints),
                    MissionExecutionDTO.DroneExecution.create("drone-2", "route-2", 50.0, 100.0, waypoints)
            );
            var mission = new MissionExecutionDTO("mission-1", drones, 1);

            var mockMission = DomainProto.Mission.newBuilder().setName("mission-1").build();
            var mockVehicle1 = DomainProto.Vehicle.newBuilder().setName("drone-1").build();
            var mockVehicle2 = DomainProto.Vehicle.newBuilder().setName("drone-2").build();
            var mockRoute = DomainProto.Route.newBuilder().setName("route-1").build();

            when(ugcsClient.isConnected()).thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.findOrCreateMission(anyString()))
                    .thenReturn(CompletableFuture.completedFuture(mockMission));
            when(ugcsClient.findRouteByName("route-1"))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(mockRoute)));
            when(ugcsClient.findRouteByName("route-2"))
                    .thenReturn(CompletableFuture.completedFuture(Optional.of(mockRoute)));
            when(ugcsClient.uploadExistingRoute(eq("drone-1"), any(DomainProto.Route.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockVehicle1));
            when(ugcsClient.uploadExistingRoute(eq("drone-2"), any(DomainProto.Route.class)))
                    .thenReturn(CompletableFuture.completedFuture(mockVehicle2));
            when(ugcsClient.createMissionVehicle(any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(true));
            when(ugcsClient.executeCommand(any(CommandRequest.class)))
                    .thenReturn(CompletableFuture.completedFuture(true));

            var result = service.process(mission).get();

            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
            assertTrue(result.message().contains("2 drones"));
        }
    }
}
