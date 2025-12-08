package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
     * Procesa un evento individual de misión completa
     *
     * @param missionComplete Datos del evento de finalización
     * @return CompletableFuture<Void> indicando el éxito del procesamiento
     */
    @Override
    public CompletableFuture<Void> process(MissionCompleteData missionComplete) {
        log.info("📥 Processing mission complete event - Vehicle: {}, Flight time: {} seconds",
                missionComplete.vehicleId(),
                missionComplete.flightTimeSeconds());

        // Ejecutar comando LAND y luego publicar a Kafka
        return executeLandCommand(missionComplete.vehicleId())
                .thenCompose(landSuccess -> {
                    if (landSuccess) {
                        log.info("✅ LAND command executed successfully for: {}",
                                missionComplete.vehicleId());
                    } else {
                        log.warn("⚠️ LAND command failed for: {} (continuing with publish)",
                                missionComplete.vehicleId());
                    }

                    // Publicar evento a Kafka independientemente del resultado de LAND
                    return publishMissionCompleteEvent(missionComplete);
                })
                .exceptionally(error -> {
                    log.error("❌ Error processing mission complete for vehicle: {}",
                            missionComplete.vehicleId(), error);

                    // Intentar publicar el evento aunque LAND haya fallado
                    publishMissionCompleteEvent(missionComplete)
                            .exceptionally(pubError -> {
                                log.error("❌ Failed to publish mission complete event", pubError);
                                return null;
                            });

                    return null;
                });
    }

    /**
     * Ejecuta el comando LAND para un vehículo
     * Convierte de CompletableFuture a CompletableFuture
     */
    private CompletableFuture<Boolean> executeLandCommand(String vehicleId) {
        log.info("🛬 Executing LAND command for vehicle: {}", vehicleId);

        CommandRequest landCommand = new CommandRequest(
                vehicleId,
                "land",
                Map.of()
        );

        return ugcsClient.executeCommand(landCommand)
                .exceptionally(error -> {
                    log.error("Failed to execute LAND command for: {}",
                            vehicleId, error);
                    return false;
                });
    }

    /**
     * Publica el evento de misión completa al topic de Kafka
     * Convierte Mono<Void> a CompletableFuture<Void>
     */
    private CompletableFuture<Void> publishMissionCompleteEvent(MissionCompleteData missionComplete) {
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
                )
                .toFuture()  // ← Convierte Mono<Void> a CompletableFuture<Void>
                .thenApply(v -> null);  // Asegurar que retorna Void
    }

}