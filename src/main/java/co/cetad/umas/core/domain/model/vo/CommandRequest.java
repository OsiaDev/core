package co.cetad.umas.core.domain.model.vo;

import java.util.Map;

public record CommandRequest(
        String vehicleId,
        String commandCode,
        Map<String, Double> arguments
) {

    public static CommandRequest simple(String vehicleId, String commandCode) {
        return new CommandRequest(vehicleId, commandCode, Map.of());
    }

    public static CommandRequest waypoint(
            String vehicleId,
            double latitude,
            double longitude,
            double altitude,
            double speed
    ) {
        return new CommandRequest(
                vehicleId,
                "waypoint",
                Map.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "altitude_agl", altitude,
                        "ground_speed", speed
                )
        );
    }

}
