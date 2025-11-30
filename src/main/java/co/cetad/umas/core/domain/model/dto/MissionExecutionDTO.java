package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * DTO para recibir comandos de ejecución de misión desde el servicio de operaciones
 * Este es el formato que llega por Kafka desde operation service
 *
 * ACTUALIZACIÓN: Ahora incluye routeId por cada dron
 */
public record MissionExecutionDTO(

        @JsonProperty("missionId") String missionId,
        @JsonProperty("drones") List<DroneExecution> drones,
        @JsonProperty("priority") Integer priority
) {
    public MissionExecutionDTO {
        Objects.requireNonNull(drones, "drone List cannot be null");
        Objects.requireNonNull(missionId, "Mission ID cannot be null");

        if (drones.isEmpty()) {
            throw new IllegalArgumentException("drone list cannot be empty");
        }
        if (missionId.isBlank()) {
            throw new IllegalArgumentException("Mission ID cannot be empty");
        }
        if (priority == null || priority < 0) {
            // Asignar prioridad por defecto
            priority = 1;
        }
    }

    /**
     * Waypoint simple que llega desde operation service
     * Solo contiene latitud y longitud en GRADOS
     * (Se convertirán a radianes internamente para UgCS)
     */
    public record SimpleWaypoint(
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude
    ) {
        public SimpleWaypoint {
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90");
            }
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180");
            }
        }

        /**
         * Convierte la latitud a radianes (requerido por UgCS)
         */
        public double latitudeRadians() {
            return Math.toRadians(latitude);
        }

        /**
         * Convierte la longitud a radianes (requerido por UgCS)
         */
        public double longitudeRadians() {
            return Math.toRadians(longitude);
        }
    }

    /**
     * Representa la ejecución de un dron específico con su ruta
     *
     * ACTUALIZACIÓN: Incluye routeId para identificar la ruta asignada
     */
    public record DroneExecution(
            @JsonProperty("vehicleId") String vehicleId,
            @JsonProperty("routeId") String routeId,
            @JsonProperty("waypoints") List<SimpleWaypoint> waypoints
    ) {
        public DroneExecution {
            Objects.requireNonNull(vehicleId, "Vehicle ID cannot be null");
            Objects.requireNonNull(waypoints, "Waypoints cannot be null");

            if (vehicleId.isBlank()) {
                throw new IllegalArgumentException("Vehicle ID cannot be empty");
            }
            // routeId puede ser null (drones sin ruta específica)
            // waypoints puede estar vacío (drones sin ruta asignada)
        }

        /**
         * Factory method para crear ejecución de dron con waypoints y routeId
         */
        public static DroneExecution create(String vehicleId, String routeId, List<SimpleWaypoint> waypoints) {
            return new DroneExecution(vehicleId, routeId, waypoints);
        }

        /**
         * Factory method para crear ejecución de dron sin waypoints ni ruta
         */
        public static DroneExecution createWithoutRoute(String vehicleId) {
            return new DroneExecution(vehicleId, null, List.of());
        }

        /**
         * Verifica si el dron tiene waypoints asignados
         */
        public boolean hasWaypoints() {
            return waypoints != null && !waypoints.isEmpty();
        }

        /**
         * Verifica si el dron tiene un routeId específico
         */
        public boolean hasRouteId() {
            return routeId != null && !routeId.isBlank();
        }
    }
}