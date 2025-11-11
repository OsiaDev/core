package co.cetad.umas.core.domain.ports.out;

import co.cetad.umas.core.domain.model.vo.TelemetryData;

import java.util.Optional;

/**
 * Outbound port for caching and retrieving drone telemetry (hexagonal architecture).
 * This abstraction allows infrastructure adapters (e.g., Redis) to provide
 * implementations without leaking details into the domain/application layers.
 */
public interface DroneCache {

    /**
     * Retrieves the last known full telemetry for a drone.
     *
     * @param droneId vehicle/drone identifier
     * @return optional with the last known telemetry if present
     */
    Optional<TelemetryData> getTelemetry(String droneId);

    /**
     * Stores the last known full telemetry for a drone.
     *
     * @param droneId  vehicle/drone identifier
     * @param telemetry telemetry to store
     */
    void setTelemetry(String droneId, TelemetryData telemetry);
}
