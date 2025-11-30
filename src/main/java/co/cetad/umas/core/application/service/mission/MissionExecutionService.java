package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

    @Value("${mission.default-altitude:50.0}")
    private Double defaultAltitude;

    @Value("${mission.default-speed:5.0}")
    private Double defaultSpeed;

    @Value("${mission.default-acceptance-radius:5.0}")
    private Double defaultAcceptanceRadius;

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
     */
    private CompletableFuture<Boolean> processMission(MissionExecutionDTO mission) {
        log.info("Processing mission: {} with {} drones",
                mission.missionId(), mission.drones().size());

        // 1. Buscar o crear la Mission en UgCS
        return ugcsClient.findOrCreateMission(mission.missionId())
                .thenCompose(ugcsMission -> {
                    log.info("✅ Mission ready: {}", mission.missionId());

                    // 2. Procesar cada dron en paralelo
                    List<CompletableFuture<Boolean>> droneProcesses = new ArrayList<>();

                    for (MissionExecutionDTO.DroneExecution drone : mission.drones()) {
                        CompletableFuture<Boolean> droneProcess = processDrone(
                                mission.missionId(),
                                ugcsMission,
                                drone
                        );
                        droneProcesses.add(droneProcess);
                    }

                    // 3. Esperar a que todos los drones se procesen
                    return CompletableFuture.allOf(
                            droneProcesses.toArray(new CompletableFuture[0])
                    ).thenApply(v -> {
                        log.info("✅ All drones processed for mission: {}", mission.missionId());
                        return true;
                    });
                });
    }

    /**
     * Procesa un dron individual: crea/busca su ruta y la sube
     */
    private CompletableFuture<Boolean> processDrone(
            String missionId,
            Object ugcsMission,
            MissionExecutionDTO.DroneExecution drone
    ) {
        log.info("Processing drone: {} with {} waypoints",
                drone.vehicleId(), drone.waypoints().size());

        // Si el dron no tiene waypoints, solo retornar true
        if (!drone.hasWaypoints()) {
            log.info("Drone {} has no waypoints, skipping route creation", drone.vehicleId());
            return CompletableFuture.completedFuture(true);
        }

        String routeName = generateRouteName(drone, missionId);

        // 1. Buscar si la ruta ya existe
        return ugcsClient.findRouteByName(routeName)
                .thenCompose(routeOpt -> {
                    if (routeOpt.isPresent()) {
                        log.info("✅ Found existing route: '{}' for drone: {}",
                                routeName, drone.vehicleId());
                        // Ruta existe, subirla al vehículo
                        return ugcsClient.uploadExistingRoute(drone.vehicleId(), routeOpt.get());
                    } else {
                        log.info("Route '{}' not found, creating new route for drone: {}",
                                routeName, drone.vehicleId());
                        // Ruta no existe, crearla
                        return ugcsClient.createAndUploadRoute(
                                ugcsMission,
                                drone.vehicleId(),
                                routeName,
                                drone.waypoints(),
                                defaultAltitude,
                                defaultSpeed
                        );
                    }
                })
                .thenCompose(uploadSuccess -> {
                    if (!uploadSuccess) {
                        throw new RuntimeException("Failed to upload route for drone: " + drone.vehicleId());
                    }
                    // 2. Ejecutar comandos para iniciar la misión en el dron
                    return executeCommandsForDrone(drone.vehicleId());
                });
    }

    /**
     * Ejecuta la secuencia de comandos para un dron:
     * 1. AUTO (modo automático)
     * 2. START_ROUTE (iniciar la ruta)
     */
    private CompletableFuture<Boolean> executeCommandsForDrone(String vehicleId) {
        log.info("🎯 Executing command sequence for drone: {}", vehicleId);

        return executeAuto(vehicleId)
                .thenCompose(v -> waitSeconds(2))
                .thenCompose(v -> executeStartRoute(vehicleId))
                .thenApply(success -> {
                    log.info("✅ Command sequence completed for drone: {}", vehicleId);
                    return true;
                });
    }

    private CompletableFuture<Boolean> executeAuto(String vehicleId) {
        log.info("📍 Executing AUTO command for drone: {}", vehicleId);
        var autoCommand = new CommandRequest(vehicleId, "auto", Map.of());
        return ugcsClient.executeCommand(autoCommand)
                .thenApply(success -> {
                    if (!success) {
                        throw new RuntimeException("AUTO command failed for: " + vehicleId);
                    }
                    log.info("✅ AUTO command executed for: {}", vehicleId);
                    return true;
                });
    }

    private CompletableFuture<Boolean> executeStartRoute(String vehicleId) {
        log.info("📍 Executing START_ROUTE command for drone: {}", vehicleId);
        var startRouteCommand = new CommandRequest(vehicleId, "start_route", Map.of());
        return ugcsClient.executeCommand(startRouteCommand)
                .thenApply(success -> {
                    if (!success) {
                        throw new RuntimeException("START_ROUTE command failed for: " + vehicleId);
                    }
                    log.info("✅ START_ROUTE command executed for: {}", vehicleId);
                    return true;
                });
    }

    private CompletableFuture<Void> waitSeconds(int seconds) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(seconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Genera un nombre único para la ruta
     * Prioriza el routeId si está disponible, sino usa missionId_vehicleId
     */
    private String generateRouteName(MissionExecutionDTO.DroneExecution drone, String missionId) {
        // Si viene routeId en el mensaje, usarlo
        if (drone.routeId() != null && !drone.routeId().isBlank()) {
            return drone.routeId();
        }
        // Fallback: generar nombre basado en missionId y vehicleId
        return String.format("%s_%s", missionId, drone.vehicleId());
    }

    private CommandResultDTO buildSuccessResult(MissionExecutionDTO mission) {
        return new CommandResultDTO(
                "mission", // Ahora es una misión completa, no un vehículo específico
                "execute_mission:" + mission.missionId(),
                CommandResultDTO.CommandStatus.SUCCESS,
                String.format("Mission executed successfully for %d drones", mission.drones().size()),
                Instant.now()
        );
    }

    private CommandResultDTO buildErrorResult(MissionExecutionDTO mission, Throwable error) {
        log.error("❌ Failed to execute mission: {}", mission.missionId(), error);

        var status = determineErrorStatus(error);
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