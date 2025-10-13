package co.cetad.umas.core.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

public record TelemetryEvent(
        @JsonProperty("vehicleId") String vehicleId,
        @JsonProperty("latitude") double latitude,
        @JsonProperty("longitude") double longitude,
        @JsonProperty("altitude") double altitude,
        @JsonProperty("speed") Double speed,
        @JsonProperty("heading") Double heading,
        @JsonProperty("batteryLevel") Double batteryLevel,
        @JsonProperty("satelliteCount") Integer satelliteCount,
        @JsonProperty("timestamp") LocalDateTime timestamp,
        @JsonProperty("additionalFields") Map<String, Object> additionalFields
) {

    public static TelemetryEvent from(TelemetryData data) {
        return new TelemetryEvent(
                data.vehicleId(),
                data.location().latitude(),
                data.location().longitude(),
                data.location().altitude(),
                data.getSpeed().orElse(null),
                data.getHeading().orElse(null),
                data.getBatteryLevel().orElse(null),
                data.getSatelliteCount().orElse(null),
                data.timestamp(),
                data.fields()
        );
    }

}