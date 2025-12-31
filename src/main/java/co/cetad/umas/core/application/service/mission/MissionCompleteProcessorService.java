package co.cetad.umas.core.application.service.mission;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.MissionCompleteData;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    private static final Duration LAND_COMMAND_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Procesa un evento individual de misión completa
     * Secuencia:
     * 1. Envía comando LAND al dron
     * 2. Publica evento a Kafka
     *
     * @param missionComplete Datos del evento de finalización
     * @return CompletableFuture<Void> indicando el éxito del procesamiento
     */
    @Override
    public CompletableFuture<Void> process(MissionCompleteData missionComplete) {
        log.info("📥 Processing mission complete event - Vehicle: {}, Flight time: {} seconds",
                missionComplete.vehicleId(),
                missionComplete.flightTimeSeconds());

        // 1. Enviar comando LAND al dron
        return sendLandCommand(missionComplete.vehicleId())
                // 2. Luego publicar evento a Kafka
                .thenCompose(landSuccess -> publishMissionCompleteEvent(missionComplete))
                .exceptionally(error -> {
                    log.error("❌ Error processing mission complete for vehicle: {}",
                            missionComplete.vehicleId(), error);
                    return null;
                });
    }

    /**
     * Envía el comando LAND al dron
     *
     * @param vehicleId ID del vehículo al que se enviará el comando
     * @return CompletableFuture<Boolean> indicando éxito del comando
     */
    private CompletableFuture<Boolean> sendLandCommand(String vehicleId) {
        log.info("🛬 Sending LAND command to vehicle: {}", vehicleId);

        CommandRequest landCommand = new CommandRequest(vehicleId, "land_command", Map.of());

        return ugcsClient.executeCommand(landCommand)
                .orTimeout(LAND_COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .whenComplete((success, error) -> {
                    if (error != null) {
                        log.error("❌ Failed to send LAND command to vehicle: {}", vehicleId, error);
                    } else if (success) {
                        log.info("✅ LAND command sent successfully to vehicle: {}", vehicleId);
                    } else {
                        log.warn("⚠️ LAND command returned false for vehicle: {}", vehicleId);
                    }
                })
                .exceptionally(error -> {
                    log.error("❌ Exception sending LAND command to vehicle: {}", vehicleId, error);
                    // Continuar con publicación aunque falle LAND
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