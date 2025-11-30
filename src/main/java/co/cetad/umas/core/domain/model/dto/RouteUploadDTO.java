package co.cetad.umas.core.domain.model.dto;

import co.cetad.umas.core.domain.model.vo.RouteDefinition;
import java.util.List;

/**
 * DTO para recibir comandos de upload de rutas desde Kafka
 */
public record RouteUploadDTO(
        String vehicleId,
        String routeName,
        List<WaypointDTO> waypoints,
        RouteSettingsDTO settings,

        Integer priority
) {
    public record WaypointDTO(
            double latitude,
            double longitude,
            double altitude,
            Double speed,
            Double heading,
            Double acceptanceRadius,
            String altitudeType  // "AGL", "AMSL", "WGS84"
    ) {
        public RouteDefinition.Waypoint toDomain() {
            var altType = altitudeType != null
                    ? RouteDefinition.Waypoint.AltitudeType.valueOf(altitudeType.toUpperCase())
                    : RouteDefinition.Waypoint.AltitudeType.AGL;

            return new RouteDefinition.Waypoint(
                    latitude,
                    longitude,
                    altitude,
                    speed,
                    heading,
                    acceptanceRadius,
                    altType
            );
        }
    }

    public record RouteSettingsDTO(
            Double defaultSpeed,
            Double defaultAltitude,
            Double defaultAcceptanceRadius,
            Boolean autoStart
    ) {
        public RouteDefinition.RouteSettings toDomain() {
            return new RouteDefinition.RouteSettings(
                    defaultSpeed != null ? defaultSpeed : 5.0,
                    defaultAltitude != null ? defaultAltitude : 50.0,
                    defaultAcceptanceRadius != null ? defaultAcceptanceRadius : 5.0,
                    autoStart != null && autoStart
            );
        }
    }

    public RouteDefinition toDomain() {
        return new RouteDefinition(
                routeName,
                vehicleId,
                waypoints.stream().map(WaypointDTO::toDomain).toList(),
                settings != null ? settings.toDomain() : RouteDefinition.RouteSettings.defaults()
        );
    }
}