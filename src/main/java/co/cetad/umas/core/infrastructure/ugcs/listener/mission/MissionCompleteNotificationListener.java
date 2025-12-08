package co.cetad.umas.core.infrastructure.ugcs.listener.mission;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import com.ugcs.ucs.client.ServerNotification;
import com.ugcs.ucs.client.ServerNotificationListener;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

@Slf4j
public record MissionCompleteNotificationListener(
        Sinks.Many<MissionCompleteData> missionCompleteSink
) implements ServerNotificationListener {

    @Override
    public void notificationReceived(ServerNotification event) {
        try {
            var wrapper = event.getEvent();
            if (wrapper == null || !wrapper.hasObjectModificationEvent()) {
                return;
            }

            var modEvent = wrapper.getObjectModificationEvent();

            // Solo procesamos eventos de eliminación de RoutePass
            if (!modEvent.getObjectType().equals("VehicleLogEntry")) {
                return;
            }

        } catch (Exception e) {
            log.error("Error processing telemetry notification", e);
        }
    }

}
