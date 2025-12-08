package co.cetad.umas.core.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MissionCompleteData(
        String vehicleId,
        DroneLocation location,
        Integer minutes,
        String message,
        String missionId,
        LocalDateTime timestamp
) {
}