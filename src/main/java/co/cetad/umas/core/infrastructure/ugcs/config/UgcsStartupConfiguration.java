package co.cetad.umas.core.infrastructure.ugcs.config;

import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Configuración de inicio que conecta a UgCS e inicia las suscripciones
 * - Telemetría de drones
 * - Eventos de misión completa
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UgcsStartupConfiguration {

    private final VehicleConnectionManager connectionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("🚀 Application ready, initializing UgCS connection and subscriptions...");

        connectionManager.connect()
                .then(startTelemetrySubscription())
                .then(startMissionCompleteSubscription())  // ← AGREGADO
                .doOnSuccess(v -> log.info("✅ UgCS connection established and all subscriptions started"))
                .doOnError(e -> log.error("❌ Failed to initialize UgCS connection", e))
                .subscribe(
                        v -> log.info("✅ All UgCS services initialized successfully"),
                        error -> log.error("❌ Fatal error during UgCS initialization", error)
                );
    }

    /**
     * Inicia la suscripción de telemetría
     */
    private Mono<Void> startTelemetrySubscription() {
        log.info("📡 Starting telemetry subscription...");
        return connectionManager.subscribeTelemetry()
                .doOnSuccess(v -> log.info("✅ Telemetry subscription active"))
                .doOnError(e -> log.error("❌ Failed to start telemetry subscription", e));
    }

    /**
     * Inicia la suscripción de eventos de misión completa
     */
    private Mono<Void> startMissionCompleteSubscription() {
        log.info("🎯 Starting mission complete event subscription...");
        return connectionManager.subscribeMissionComplete()
                .doOnSuccess(v -> log.info("✅ Mission complete subscription active"))
                .doOnError(e -> log.error("❌ Failed to start mission complete subscription", e));
    }

}