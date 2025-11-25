package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RouteExecutionDTO(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("missionId") String missionId,
        @JsonProperty("waypoints") List<WaypointDTO> waypoints,
        @JsonProperty("priority") Integer priority
) {

}