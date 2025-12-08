package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio que procesa eventos de finalización de misión
 * Orquesta:
 * 1. Envío de comando LAND al dron
 * 2. Publicación del evento a Kafka (topic: umas.drone.mission.status)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionCompleteProcessorService implements EventProcessor<MissionCompleteData, Void> {

    private final UgcsClient ugcsClient;
    private final EventPublisher<MissionCompleteData> missionCompletePublisher;

    /**
     * Inicia la escucha de eventos de misión completa
     * Procesa cada evento ejecutando el comando LAND y publicando a Kafka
     */


    /**
     * Procesa un evento individual de misión completa
     *
     * @param missionComplete Datos del evento de finalización
     * @return Mono<Boolean> indicando el éxito del procesamiento
     */
    public CompletableFuture<Void> process(MissionCompleteData missionComplete) {
        log.info("📥 Processing mission complete event - Vehicle: {}, Flight time: {} seconds",
                missionComplete.vehicleId(),
                missionComplete.flightTimeSeconds());

        return executeLandCommand(missionComplete.vehicleId())
                .flatMap(landSuccess -> {
                    if (landSuccess) {
                        log.info("✅ LAND command executed successfully for: {}",
                                missionComplete.vehicleId());
                        return publishMissionCompleteEvent(missionComplete)
                                .thenReturn(true);
                    } else {
                        log.warn("⚠️ LAND command failed for: {}",
                                missionComplete.vehicleId());
                        // Publicamos el evento aunque el LAND falle
                        return publishMissionCompleteEvent(missionComplete)
                                .thenReturn(false);
                    }
                })
                .doOnError(error ->
                        log.error("❌ Error processing mission complete for vehicle: {}",
                                missionComplete.vehicleId(), error)
                )
                .onErrorReturn(false);
    }

    /**
     * Ejecuta el comando LAND para un vehículo
     */
    private Mono<Boolean> executeLandCommand(String vehicleId) {
        log.info("🛬 Executing LAND command for vehicle: {}", vehicleId);

        CommandRequest landCommand = new CommandRequest(
                vehicleId,
                "land",
                Map.of()
        );

        return Mono.fromFuture(() ->
                ugcsClient.executeCommand(landCommand)
                        .exceptionally(error -> {
                            log.error("Failed to execute LAND command for: {}",
                                    vehicleId, error);
                            return false;
                        })
        );
    }

    /**
     * Publica el evento de misión completa al topic de Kafka
     */
    private Mono<Void> publishMissionCompleteEvent(MissionCompleteData missionComplete) {
        log.info("📤 Publishing mission complete event to Kafka - Vehicle: {}",
                missionComplete.vehicleId());

        return missionCompletePublisher.publish(missionComplete)
                .doOnSuccess(v ->
                        log.info("✅ Mission complete event published successfully for: {}",
                                missionComplete.vehicleId())
                )
                .doOnError(error ->
                        log.error("❌ Failed to publish mission complete event for: {}",
                                missionComplete.vehicleId(), error)
                );
    }

}