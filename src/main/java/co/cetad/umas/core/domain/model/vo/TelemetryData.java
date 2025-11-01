package co.cetad.umas.core.domain.model.vo;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public record TelemetryData(
        String vehicleId,
        DroneLocation location,
        Map<String, Object> fields,
        LocalDateTime timestamp
) {

    public Optional<Double> getSpeed() {
        return Optional.ofNullable(fields.get("groundSpeed"))
                .map(v -> v instanceof Number n ? n.doubleValue() : null);
    }

    public Optional<Double> getBatteryLevel() {
        return Optional.ofNullable(fields.get("batteryLevel"))
                .map(v -> v instanceof Number n ? n.doubleValue() : null);
    }

    public Optional<Double> getHeading() {
        return Optional.ofNullable(fields.get("heading"))
                .map(v -> v instanceof Number n ? n.doubleValue() : null);
    }

    public Optional<Integer> getSatelliteCount() {
        return Optional.ofNullable(fields.get("satelliteCount"))
                .map(v -> v instanceof Number n ? n.intValue() : null);
    }

    /**
     * Returns the location only when it is valid (non-null and non-zero lat/lon).
     * UgCS reports 0.0 when a coordinate wasn't present; we treat that as invalid.
     */
    public Optional<DroneLocation> isNewDroneLocationValid() {
        return Optional.ofNullable(location)
                .filter(loc -> loc.latitude() != 0.0 && loc.longitude() != 0.0);
    }
}