package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record CommandResultDTO(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("commandCode") String commandCode,
        @JsonProperty("status") CommandStatus status,
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") Instant timestamp
) {

    public enum CommandStatus {
        SUCCESS,
        FAILED,
        REJECTED,
        TIMEOUT
    }

    public static CommandResultDTO success(String vehicleId, String commandCode) {
        return new CommandResultDTO(
                vehicleId,
                commandCode,
                CommandStatus.SUCCESS,
                "Command executed successfully",
                Instant.now()
        );
    }

    public static CommandResultDTO failed(String vehicleId, String commandCode, String message) {
        return new CommandResultDTO(
                vehicleId,
                commandCode,
                CommandStatus.FAILED,
                message,
                Instant.now()
        );
    }

}