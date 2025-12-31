package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import com.ugcs.ucs.proto.DomainProto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Servicio principal para ejecutar misiones con múltiples drones
 *
 * Flujo:
 * 1. Recibe MissionExecutionDTO con lista de drones y sus waypoints
 * 2. Verifica si la misión ya existe en UgCS
 * 3. Para cada dron:
 *    - Busca o crea su ruta dentro de la misión
 *    - Sube la ruta al dron
 * 4. Ejecuta comandos para cada dron: AUTO -> START_ROUTE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionExecutionService implements EventProcessor<MissionExecutionDTO, CommandResultDTO> {

    private final UgcsClient ugcsClient;

    @Value("${mission.default-speed:5.0}")
    private Double defaultSpeed;

    private static final Duration MISSION_EXECUTION_TIMEOUT = Duration.ofMinutes(5);

    @Override
    public CompletableFuture<CommandResultDTO> process(MissionExecutionDTO mission) {
        log.info("🚀 Processing mission execution: {} for {} drones",
                mission.missionId(), mission.drones().size());

        return validateConnection()
                .thenCompose(v -> processMission(mission))
                .thenApply(success -> buildSuccessResult(mission))
                .orTimeout(MISSION_EXECUTION_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .exceptionally(error -> buildErrorResult(mission, error));
    }

    private CompletableFuture<Void> validateConnection() {
        return ugcsClient.isConnected()
                .thenAccept(connected -> {
                    if (!connected) {
                        throw new IllegalStateException("Not connected to UgCS Server");
                    }
                });
    }

    /**
     * Procesa toda la misión: crea/busca la mission y procesa cada dron
     * CORREGIDO: Ahora procesa correctamente cada dron de forma asíncrona
     */
    private CompletableFuture<Boolean> processMission(MissionExecutionDTO mission) {
        log.info("Processing mission: {} with {} drones",
                mission.missionId(), mission.drones().size());

        // 1. Buscar o crear la Mission en UgCS
        return ugcsClient.findOrCreateMission(mission.missionId())
                .thenCompose(ugcsMission -> {
                    log.info("✅ Mission ready: {}", mission.missionId());

                    // 2. Procesar cada dron de forma asíncrona y recolectar los CompletableFutures
                    List<CompletableFuture<Boolean>> droneProcessingFutures = mission.drones().stream()
                            .map(drone -> processSingleDrone(ugcsMission, drone))
                            .toList();

                    // 3. Esperar a que todos los drones se procesen con allOf
                    return CompletableFuture.allOf(
                            droneProcessingFutures.toArray(new CompletableFuture[0])
                    ).thenApply(v -> {
                        log.info("✅ All {} drones processed for mission: {}",
                                mission.drones().size(), mission.missionId());
                        return true;
                    });
                });
    }

    /**
     * Procesa un dron individual de forma completa:
     * 1. Crea/sube la ruta
     * 2. Registra el vehículo en la misión
     * 3. Ejecuta comandos
     *
     * @return CompletableFuture<Boolean> indicando éxito del procesamiento
     */
    private CompletableFuture<Boolean> processSingleDrone(
            DomainProto.Mission ugcsMission,
            MissionExecutionDTO.DroneExecution drone
    ) {
        log.info("📍 Processing drone: {} with {} waypoints",
                drone.vehicleId(), drone.waypoints().size());

        // 1. Procesar la ruta del dron
        return processDroneRoute(ugcsMission, drone)
                .thenCompose(vehicle -> {
                    // 2. Si no hay vehicle (sin waypoints), completar sin registrar
                    if (vehicle == null) {
                        log.info("An error occurred processing the route for vehicle {}", drone.vehicleId());
                        return CompletableFuture.completedFuture(true);
                    }

                    // 3. Registrar el vehículo en la misión
                    return registerVehicleInMission(ugcsMission, vehicle)
                            .thenCompose(registered -> {
                                // 4. Ejecutar comandos para el dron
                                if (registered) {
                                    return executeCommandsForDrone(drone.vehicleId());
                                }
                                log.warn("Failed to register vehicle {} in mission", drone.vehicleId());
                                return CompletableFuture.completedFuture(false);
                            });
                });
    }

    /**
     * Procesa la ruta de un dron: crea o busca la ruta y la sube al vehículo
     *
     * @return CompletableFuture<DomainProto.Vehicle> el vehículo con la ruta cargada, o null si no hay waypoints
     */
    private CompletableFuture<DomainProto.Vehicle> processDroneRoute(
            DomainProto.Mission ugcsMission,
            MissionExecutionDTO.DroneExecution drone
    ) {
        // Si el dron no tiene waypoints, retornar null
        if (!drone.hasWaypoints()) {
            log.info("Drone {} has no waypoints, skipping route creation", drone.vehicleId());
            return CompletableFuture.completedFuture(null);
        }

        // Buscar si la ruta ya existe
        return ugcsClient.findRouteByName(drone.routeId())
                .thenCompose(existingRoute -> {
                    if (existingRoute.isPresent()) {
                        log.info("✅ Found existing route: {}, uploading to drone", drone.routeId());
                        return ugcsClient.uploadExistingRoute(drone.vehicleId(), existingRoute.get());
                    } else {
                        log.info("Creating new route: {} for drone: {}", drone.routeId(), drone.vehicleId());
                        return ugcsClient.createAndUploadRoute(ugcsMission, drone, defaultSpeed);
                    }
                });
    }

    /**
     * Registra un vehículo en una misión
     */
    private CompletableFuture<Boolean> registerVehicleInMission(
            DomainProto.Mission ugcsMission,
            DomainProto.Vehicle vehicle
    ) {
        log.info("📝 Registering vehicle {} in mission", vehicle.getName());
        return ugcsClient.createMissionVehicle(ugcsMission, vehicle)
                .thenApply(success -> {
                    if (success) {
                        log.info("✅ Vehicle {} registered in mission", vehicle.getName());
                    } else {
                        log.warn("⚠️ Failed to register vehicle {} in mission", vehicle.getName());
                    }
                    return success;
                });
    }

    /**
     * Ejecuta la secuencia de comandos para un dron:
     * 1. AUTO (modo automático)
     *
     * Puede extenderse fácilmente para agregar más comandos en secuencia
     */
    private CompletableFuture<Boolean> executeCommandsForDrone(String vehicleId) {
        log.info("🎯 Executing command sequence for drone: {}", vehicleId);

        return executeAutoCommand(vehicleId)
                .thenApply(success -> {
                    log.info("✅ Command sequence completed for drone: {}", vehicleId);
                    return success;
                });
    }

    /**
     * Ejecuta el comando AUTO para un dron
     */
    private CompletableFuture<Boolean> executeAutoCommand(String vehicleId) {
        log.info("📍 Executing AUTO command for drone: {}", vehicleId);

        CommandRequest autoCommand = new CommandRequest(vehicleId, "auto", Map.of());

        return ugcsClient.executeCommand(autoCommand)
                .thenApply(success -> {
                    if (!success) {
                        throw new RuntimeException("AUTO command failed for: " + vehicleId);
                    }
                    log.info("✅ AUTO command executed for: {}", vehicleId);
                    return true;
                });
    }

    private CommandResultDTO buildSuccessResult(MissionExecutionDTO mission) {
        return new CommandResultDTO(
                "mission",
                "execute_mission:" + mission.missionId(),
                CommandResultDTO.CommandStatus.SUCCESS,
                String.format("Mission executed successfully for %d drones", mission.drones().size()),
                Instant.now()
        );
    }

    private CommandResultDTO buildErrorResult(MissionExecutionDTO mission, Throwable error) {
        log.error("❌ Failed to execute mission: {}", mission.missionId(), error);

        CommandResultDTO.CommandStatus status = determineErrorStatus(error);
        return new CommandResultDTO(
                "mission",
                "execute_mission:" + mission.missionId(),
                status,
                error.getMessage(),
                Instant.now()
        );
    }

    private CommandResultDTO.CommandStatus determineErrorStatus(Throwable error) {
        if (error instanceof TimeoutException) {
            return CommandResultDTO.CommandStatus.TIMEOUT;
        }
        if (error instanceof IllegalStateException || error instanceof IllegalArgumentException) {
            return CommandResultDTO.CommandStatus.REJECTED;
        }
        return CommandResultDTO.CommandStatus.FAILED;
    }

}