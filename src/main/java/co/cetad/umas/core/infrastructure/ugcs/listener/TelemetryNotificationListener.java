package co.cetad.umas.core.infrastructure.ugcs.listener;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import com.ugcs.ucs.client.ServerNotification;
import com.ugcs.ucs.client.ServerNotificationListener;
import com.ugcs.ucs.proto.DomainProto;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public record TelemetryNotificationListener(
        Sinks.Many<TelemetryData> telemetrySink,
        co.cetad.umas.core.domain.ports.out.DroneCache droneCache
) implements ServerNotificationListener {

    @Override
    public void notificationReceived(ServerNotification event) {
        try {
            var wrapper = event.getEvent();
            if (wrapper == null || !wrapper.hasTelemetryEvent()) {
                return;
            }

            var telemetryEvent = wrapper.getTelemetryEvent();
            var vehicle = telemetryEvent.getVehicle();

            final var telemetryData = processTelemetry(
                    vehicle.getName(),
                    telemetryEvent.getTelemetryList()
            );

            // Validate latitude/longitude before emitting to Kafka using TelemetryData API
            var droneLocationValid = telemetryData.isNewDroneLocationValid();

            droneLocationValid.ifPresentOrElse(
                    onNewDroneLocation(telemetryData),
                    emptyDroneLocation(telemetryData)
            );

        } catch (Exception e) {
            log.error("Error processing telemetry notification", e);
        }
    }

    @NotNull
    private Runnable emptyDroneLocation(TelemetryData telemetryData) {
        return () -> {
            // Invalid location: try to enrich from cache replacing zero coords with cached ones
            var cached = droneCache.getTelemetry(telemetryData.vehicleId());

            var newDrone = cached.map(droneCached -> new TelemetryData(
                    droneCached.vehicleId(),
                    droneCached.location(),
                    telemetryData.fields(),
                    telemetryData.timestamp()
            ));

            newDrone.ifPresent(drone -> droneCache.setTelemetry(telemetryData.vehicleId(), drone));
            // Keep current behavior: do not emit when invalid
            log.debug("Skipping telemetry emission for vehicle {} due to invalid lat/lon and no cache", telemetryData.vehicleId());
        };
    }

    @NotNull
    private Consumer<DroneLocation> onNewDroneLocation(TelemetryData telemetryData) {
        return loc -> {
            var droneOld = droneCache.getTelemetry(telemetryData.vehicleId());
            var newDrone = droneOld.map(droned -> {
                var latitude = loc.latitude();
                if (latitude == 0.0) {
                    latitude = droned.location().latitude();
                }
                var longitude = loc.longitude();
                if (longitude == 0.0) {
                    longitude = droned.location().longitude();
                }
                return new TelemetryData(
                        droned.vehicleId(),
                        DroneLocation.of(latitude, longitude, telemetryData.location().altitude()),
                        telemetryData.fields(),
                        telemetryData.timestamp()
                );
            });
            newDrone.ifPresentOrElse(drone -> {
                // Valid location: update cache and emit
                droneCache.setTelemetry(telemetryData.vehicleId(), drone);

                var emitResult = telemetrySink.tryEmitNext(drone);
                if (emitResult.isFailure()) {
                    log.warn("Failed to emit telemetry: {}", emitResult);
                }
            }, () -> {
                droneCache.setTelemetry(telemetryData.vehicleId(), telemetryData);
            });
        };
    }

    private TelemetryData processTelemetry(
            String vehicleId,
            List<DomainProto.Telemetry> telemetryList
    ) {
        Map<String, Object> fields = new HashMap<>();
        final double[] latRef = new double[]{0.0};
        final double[] lonRef = new double[]{0.0};
        final double[] altRef = new double[]{0.0};

        for (var telemetry : telemetryList) {
            var field = telemetry.getTelemetryField();
            var value = telemetry.getValue();

            processTelemetryField(field, value, fields,
                    v -> {
                        latRef[0] = v;
                    },
                    v -> {
                        lonRef[0] = v;
                    },
                    v -> altRef[0] = v
            );
        }

        return new TelemetryData(
                vehicleId,
                DroneLocation.of(latRef[0], lonRef[0], altRef[0]),
                fields,
                LocalDateTime.now()
        );
    }

    private void processTelemetryField(
            DomainProto.TelemetryField field,
            DomainProto.Value value,
            Map<String, Object> fields,
            Consumer<Double> latConsumer,
            Consumer<Double> lonConsumer,
            Consumer<Double> altConsumer
    ) {
        switch (field.getSemantic()) {
            case S_LATITUDE -> latConsumer.accept(Math.toDegrees(value.getDoubleValue()));
            case S_LONGITUDE -> lonConsumer.accept(Math.toDegrees(value.getDoubleValue()));
            case S_ALTITUDE_AGL -> altConsumer.accept(value.getDoubleValue());
            case S_ALTITUDE_AMSL -> fields.put("altitudeAmsl", value.getDoubleValue());
            case S_GROUND_SPEED -> fields.put("groundSpeed", value.getDoubleValue());
            case S_AIR_SPEED -> fields.put("airSpeed", value.getDoubleValue());
            case S_VERTICAL_SPEED -> fields.put("verticalSpeed", value.getDoubleValue());
            case S_HEADING -> fields.put("heading", value.getDoubleValue());
            case S_VOLTAGE -> fields.put("batteryLevel", calculateBatteryLevel(value.getDoubleValue()));
            case S_CURRENT -> fields.put("current", value.getDoubleValue());
            case S_SATELLITE_COUNT -> fields.put("satelliteCount", value.getIntValue());
            case S_GPS_FIX_TYPE -> fields.put("gpsFixType", value.getIntValue());
            case S_ROLL -> fields.put("roll", value.getDoubleValue());
            case S_PITCH -> fields.put("pitch", value.getDoubleValue());
            case S_YAW -> fields.put("yaw", value.getDoubleValue());
            case S_RC_LINK_QUALITY -> fields.put("rcLinkQuality", value.getDoubleValue());
            case S_GCS_LINK_QUALITY -> fields.put("gcsLinkQuality", value.getDoubleValue());
            case S_CONTROL_MODE -> fields.put("controlMode", extractValue(value));
            case S_FLIGHT_MODE -> fields.put("flightMode", extractValue(value));
            case S_GROUND_ELEVATION -> fields.put("groundElevation", value.getDoubleValue());
            default -> {
                // Store other fields with their code
                if (field.hasCode()) {
                    fields.put(field.getCode(), extractValue(value));
                }
            }
        }
    }

    private double calculateBatteryLevel(double voltage) {
        // Default battery calculation for 4S LiPo (adjust based on your drone specs)
        double minVoltage = 14.8; // 4S LiPo minimum (3.7V per cell)
        double maxVoltage = 16.8; // 4S LiPo maximum (4.2V per cell)

        // For 3S: min=11.1, max=12.6
        // For 6S: min=22.2, max=25.2

        double percentage = ((voltage - minVoltage) / (maxVoltage - minVoltage)) * 100;
        return Math.max(0, Math.min(100, percentage));
    }

    private Object extractValue(DomainProto.Value value) {
        if (value.hasBoolValue()) return value.getBoolValue();
        if (value.hasIntValue()) return value.getIntValue();
        if (value.hasLongValue()) return value.getLongValue();
        if (value.hasFloatValue()) return value.getFloatValue();
        if (value.hasDoubleValue()) return value.getDoubleValue();
        if (value.hasStringValue()) return value.getStringValue();
        return null;
    }

}
