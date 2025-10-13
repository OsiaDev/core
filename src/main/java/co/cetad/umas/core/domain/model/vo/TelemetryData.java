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

}