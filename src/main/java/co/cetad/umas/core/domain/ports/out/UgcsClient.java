package co.cetad.umas.core.domain.ports.out;

import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import com.ugcs.ucs.proto.DomainProto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UgcsClient {

    // Conexión
    Mono<Void> connect(String host, int port, String username, String password);
    Mono<Void> disconnect();
    CompletableFuture<Boolean> isConnected();

    // Telemetría
    Flux<TelemetryData> subscribeTelemetry();

    Flux<MissionCompleteData> subscribeMissionComplete();

    // Comandos
    CompletableFuture<Boolean> executeCommand(CommandRequest command);

    // Misiones y Rutas
    /**
     * Busca o crea una misión en UgCS
     * @param missionName Nombre de la misión
     * @return La misión de UgCS (DomainProto.Mission)
     */
    CompletableFuture<DomainProto.Mission> findOrCreateMission(String missionName);

    /**
     * Busca una ruta por nombre
     * @param routeName Nombre de la ruta
     * @return Optional con la ruta si existe
     */
    CompletableFuture<Optional<DomainProto.Route>> findRouteByName(String routeName);


    CompletableFuture<Boolean> createMissionVehicle(
            DomainProto.Mission ugcsMission,
            DomainProto.Vehicle vehicle
    );

    /**
     * Crea una nueva ruta y la sube al vehículo
     *
     * @param ugcsMission  Misión de UgCS a la que pertenece
     * @param drone        DroneExecution del vehículo
     * @param defaultSpeed Velocidad
     * @return true si se creó y subió exitosamente
     */
    CompletableFuture<DomainProto.Vehicle> createAndUploadRoute(
            DomainProto.Mission ugcsMission,
            MissionExecutionDTO.DroneExecution drone,
            Double defaultSpeed);

    /**
     * Sube una ruta existente a un vehículo
     * @param vehicleId ID del vehículo
     * @param existingRoute Ruta existente (DomainProto.Route)
     * @return true si se subió exitosamente
     */
    CompletableFuture<DomainProto.Vehicle> uploadExistingRoute(String vehicleId, DomainProto.Route existingRoute);

    /**
     * Información de una ruta
     */
    record RouteInfo(String name, int waypointCount, Object ugcsRoute) {}

    /**
     * Obtiene todas las rutas de un vehículo
     */
    CompletableFuture<List<RouteInfo>> getVehicleRoutes(String vehicleId);
}