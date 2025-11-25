package co.cetad.umas.core.domain.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record RouteExecutionResult(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("routeId") String routeId,
        @JsonProperty("status") RouteStatus status,
        @JsonProperty("completedCommands") Integer completedCommands,
        @JsonProperty("totalCommands") Integer totalCommands,
        @JsonProperty("currentCommand") String currentCommand,
        @JsonProperty("message") String message,
        @JsonProperty("errors") List<String> errors,
        @JsonProperty("startTime") Instant startTime,
        @JsonProperty("endTime") Instant endTime
) {

    public enum RouteStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED,
        TIMEOUT
    }

    public static RouteExecutionResult inProgress(String vehicleId, String routeId,
                                                  int completed, int total, String currentCmd) {
        return new RouteExecutionResult(
                vehicleId,
                routeId,
                RouteStatus.IN_PROGRESS,
                completed,
                total,
                currentCmd,
                String.format("Executing command %d of %d: %s", completed + 1, total, currentCmd),
                List.of(),
                Instant.now(),
                null
        );
    }

    public static RouteExecutionResult completed(String vehicleId, String routeId,
                                                 int total, Instant startTime) {
        return new RouteExecutionResult(
                vehicleId,
                routeId,
                RouteStatus.COMPLETED,
                total,
                total,
                null,
                "Route execution completed successfully",
                List.of(),
                startTime,
                Instant.now()
        );
    }

    public static RouteExecutionResult failed(String vehicleId, String routeId,
                                              int completed, int total,
                                              String failedCommand, String error,
                                              Instant startTime) {
        return new RouteExecutionResult(
                vehicleId,
                routeId,
                RouteStatus.FAILED,
                completed,
                total,
                failedCommand,
                String.format("Route execution failed at command %d: %s", completed + 1, failedCommand),
                List.of(error),
                startTime,
                Instant.now()
        );
    }

}