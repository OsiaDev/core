package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RouteExecutionDTO(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("routeName") String routeName,
        @JsonProperty("waypoints") List<WaypointDTO> waypoints,
        @JsonProperty("speed") Double speed,
        @JsonProperty("altitude") Double altitude
) {

    public record WaypointDTO(
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("altitude") double altitude,
            @JsonProperty("speed") Double speed
    ) {}

}