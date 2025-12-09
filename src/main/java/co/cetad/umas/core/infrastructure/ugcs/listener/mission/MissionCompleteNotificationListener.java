package co.cetad.umas.core.infrastructure.ugcs.listener.mission;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import com.ugcs.ucs.client.ServerNotification;
import com.ugcs.ucs.client.ServerNotificationListener;
import com.ugcs.ucs.proto.DomainProto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

@Slf4j
public record MissionCompleteNotificationListener(
        Sinks.Many<MissionCompleteData> missionCompleteSink,
        DroneCache droneCache
) implements ServerNotificationListener {

    private static final String MISSION_COMPLETE_MESSAGE = "Current mission complete";
    private static final String OBJECT_TYPE = "VehicleLogEntry";

    @Override
    public void notificationReceived(ServerNotification event) {
        try {
            var wrapper = event.getEvent();
            if (wrapper == null || !wrapper.hasObjectModificationEvent()) {
                return;
            }

            var modEvent = wrapper.getObjectModificationEvent();

            // Solo procesamos eventos de creación de VehicleLogEntry
            if (!modEvent.getObjectType().equals(OBJECT_TYPE)) {
                return;
            }

            if (!modEvent.getModificationType().equals(
                    DomainProto.ModificationType.MT_CREATE)) {
                return;
            }

            // Obtener el objeto VehicleLogEntry
            if (!modEvent.hasObject() || !modEvent.getObject().hasVehicleLogEntry()) {
                return;
            }

            var logEntry = modEvent.getObject().getVehicleLogEntry();

            // Verificar si el mensaje indica finalización de misión
            if (!logEntry.hasMessage() ||
                    !logEntry.getMessage().contains(MISSION_COMPLETE_MESSAGE)) {
                return;
            }

            // Extraer información del evento
            String vehicleId = extractVehicleId(logEntry);
            String message = logEntry.getMessage();
            long timeMillis = logEntry.hasTime() ? logEntry.getTime() : System.currentTimeMillis();

            log.info("🎯 Mission complete detected - Vehicle: {}, Message: {}",
                    vehicleId, message);

            // Crear el objeto de datos de misión completa
            MissionCompleteData missionComplete = createMissionCompleteData(
                    vehicleId,
                    message,
                    timeMillis
            );

            // Emitir el evento
            Sinks.EmitResult result = missionCompleteSink.tryEmitNext(missionComplete);

            if (result.isFailure()) {
                log.error("Failed to emit mission complete event: {}", result);
            } else {
                log.info("✅ Mission complete event emitted for vehicle: {}", vehicleId);
            }

        } catch (Exception e) {
            log.error("Error processing mission complete notification", e);
        }
    }

    /**
     * Crea MissionCompleteData intentando obtener la ubicación del dron
     */
    private MissionCompleteData createMissionCompleteData(
            String vehicleId,
            String message,
            long timeMillis
    ) {
        // Intentar obtener la última ubicación conocida del dron desde el cache
        DroneLocation location = null;

        try {
            var telemetry = droneCache.getTelemetry(vehicleId);
            if (telemetry.isPresent()) {
                location = telemetry.map(TelemetryData::location).get();
                log.debug("Using cached location for vehicle {}: lat={}, lon={}",
                        vehicleId, location.latitude(), location.longitude());
            } else {
                log.debug("No cached location available for vehicle: {}", vehicleId);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve location from cache for vehicle: {}", vehicleId, e);
        }

        // Crear con o sin location según disponibilidad
        Double flightTime = extractFlightTime(message);

        return new MissionCompleteData(
                vehicleId,
                location,  // Puede ser null si no hay ubicación disponible
                flightTime,
                message,
                null,  // missionId se asignará después
                java.time.Instant.ofEpochMilli(timeMillis)
        );
    }

    /**
     * Extrae el vehicleId del VehicleLogEntry
     */
    private String extractVehicleId(DomainProto.VehicleLogEntry logEntry) {
        if (logEntry.hasVehicle() && logEntry.getVehicle().hasName()) {
            return logEntry.getVehicle().getName();
        }
        return "UNKNOWN";
    }

    /**
     * Extrae el tiempo de vuelo del mensaje del log
     */
    private Double extractFlightTime(String message) {
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
            log.warn("Failed to parse flight time from message: {}", message, e);
            return null;
        }

        return null;
    }

}