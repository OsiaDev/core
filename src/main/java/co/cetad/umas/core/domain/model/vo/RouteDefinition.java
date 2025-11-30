package co.cetad.umas.core.domain.model.vo;

import java.util.List;

/**
 * Representa una ruta completa con múltiples waypoints
 */
public record RouteDefinition(
        String routeName,
        String vehicleId,
        List<Waypoint> waypoints,
        RouteSettings settings
) {
    public record Waypoint(
            double latitude,      // grados
            double longitude,     // grados
            double altitude,      // metros AGL (Above Ground Level)
            Double speed,         // m/s (opcional)
            Double heading,       // grados (opcional)
            Double acceptanceRadius, // metros (opcional)
            AltitudeType altitudeType
    ) {
        public Waypoint {
            if (latitude < -90 || latitude > 90) {
                throw new IllegalArgumentException("Invalid latitude: " + latitude);
            }
            if (longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Invalid longitude: " + longitude);
            }
            if (altitude < 0) {
                throw new IllegalArgumentException("Altitude cannot be negative: " + altitude);
            }
        }

        public enum AltitudeType {
            AGL,    // Above Ground Level
            AMSL,   // Above Mean Sea Level
            WGS84   // WGS84 ellipsoid
        }
    }

    public record RouteSettings(
            Double defaultSpeed,           // m/s
            Double defaultAltitude,        // metros
            Double defaultAcceptanceRadius, // metros
            boolean autoStart              // iniciar automáticamente después de upload
    ) {
        public static RouteSettings defaults() {
            return new RouteSettings(
                    5.0,    // 5 m/s velocidad por defecto
                    50.0,   // 50 metros altitud por defecto
                    5.0,    // 5 metros radio de aceptación
                    false   // no iniciar automáticamente
            );
        }
    }
}