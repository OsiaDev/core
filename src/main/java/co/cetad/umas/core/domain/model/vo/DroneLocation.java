package co.cetad.umas.core.domain.model.vo;

import java.time.LocalDateTime;

public record DroneLocation(
        double latitude,
        double longitude,
        double altitude,
        LocalDateTime timestamp
) {

    public static DroneLocation of(double lat, double lon, double alt) {
        return new DroneLocation(lat, lon, alt, LocalDateTime.now());
    }

}
