package co.cetad.umas.core.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Value Object que representa la finalización de una misión
 * Contiene información extraída del VehicleLogEntry de UgCS
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MissionCompleteData(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("flightTimeSeconds") Double flightTimeSeconds,
        @JsonProperty("message") String message,
        @JsonProperty("missionId") String missionId,
        @JsonProperty("timestamp") Instant timestamp
) {
    /**
     * Constructor de fábrica para crear desde un VehicleLogEntry de UgCS
     */
    public static MissionCompleteData fromVehicleLog(
            String vehicleId,
            String message,
            long timeMillis
    ) {
        Double flightTime = extractFlightTime(message);

        return new MissionCompleteData(
                vehicleId,
                flightTime,
                message,
                null, // Se asignará después cuando se conozca el missionId
                Instant.ofEpochMilli(timeMillis)
        );
    }

    /**
     * Extrae el tiempo de vuelo del mensaje del log
     * Formato esperado: "Current mission complete. Flight time: 93.46499991416931"
     */
    private static Double extractFlightTime(String message) {
        if (message == null || !message.contains("Flight time:")) {
            return null;
        }

        try {
            String[] parts = message.split("Flight time:");
            if (parts.length > 1) {
                String timeStr = parts[1].trim();
                return Double.parseDouble(timeStr);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    /**
     * Crea una copia con el missionId asignado
     */
    public MissionCompleteData withMissionId(String missionId) {
        return new MissionCompleteData(
                vehicleId,
                flightTimeSeconds,
                message,
                missionId,
                timestamp
        );
    }

}