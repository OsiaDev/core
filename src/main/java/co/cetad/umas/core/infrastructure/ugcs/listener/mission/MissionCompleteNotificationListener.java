package co.cetad.umas.core.infrastructure.ugcs.listener.mission;

import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import com.ugcs.ucs.client.ServerNotification;
import com.ugcs.ucs.client.ServerNotificationListener;
import com.ugcs.ucs.proto.DomainProto;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

/**
 * Listener que procesa eventos de VehicleLogEntry para detectar
 * la finalización de misiones basándose en mensajes del log
 */
@Slf4j
public record MissionCompleteNotificationListener(
        Sinks.Many<MissionCompleteData> missionCompleteSink
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
            MissionCompleteData missionComplete = MissionCompleteData.fromVehicleLog(
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
     * Extrae el vehicleId del VehicleLogEntry
     */
    private String extractVehicleId(DomainProto.VehicleLogEntry logEntry) {
        if (logEntry.hasVehicle() && logEntry.getVehicle().hasName()) {
            return logEntry.getVehicle().getName();
        }
        return "UNKNOWN";
    }

}