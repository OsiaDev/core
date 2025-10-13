package co.cetad.umas.core.domain.model.dto;

import co.cetad.umas.core.domain.model.vo.VehicleState;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record VehicleStatusDTO(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("state") VehicleState state,
        @JsonProperty("connected") boolean connected,
        @JsonProperty("lastUpdate") Instant lastUpdate,
        @JsonProperty("errorMessage") String errorMessage
) {

    public static VehicleStatusDTO connected(String vehicleId) {
        return new VehicleStatusDTO(
                vehicleId,
                VehicleState.IDLE,
                true,
                Instant.now(),
                null
        );
    }

    public static VehicleStatusDTO error(String vehicleId, String errorMessage) {
        return new VehicleStatusDTO(
                vehicleId,
                VehicleState.ERROR,
                false,
                Instant.now(),
                errorMessage
        );
    }

}