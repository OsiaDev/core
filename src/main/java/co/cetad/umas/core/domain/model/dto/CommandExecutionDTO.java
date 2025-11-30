package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record CommandExecutionDTO(
        @JsonProperty("vehicleId") String vehicleId,

        @JsonProperty("routeId") String missionId,
        @JsonProperty("commandCode") String commandCode,
        @JsonProperty("arguments") Map<String, Double> arguments,
        @JsonProperty("priority") Integer priority
) {

    public CommandExecutionDTO {
        if (vehicleId == null || vehicleId.isBlank()) {
            throw new IllegalArgumentException("vehicleId cannot be null or empty");
        }
        if (commandCode == null || commandCode.isBlank()) {
            throw new IllegalArgumentException("commandCode cannot be null or empty");
        }
        if (arguments == null) {
            arguments = Map.of();
        }
        if (priority == null) {
            priority = 0;
        }
    }

}