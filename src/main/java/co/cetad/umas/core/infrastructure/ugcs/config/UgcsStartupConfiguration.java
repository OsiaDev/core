package co.cetad.umas.core.infrastructure.ugcs.config;

import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UgcsStartupConfiguration {

    private final VehicleConnectionManager connectionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready, initializing UgCS connection...");

        connectionManager.connect()
                .then(startTelemetrySubscription())
                .doOnSuccess(v -> log.info("UgCS connection established and telemetry subscription started"))
                .doOnError(e -> log.error("Failed to initialize UgCS connection", e))
                .subscribe();
    }

    private Mono<Void> startTelemetrySubscription() {
        return this.connectionManager.subscribeTelemetry();
    }

}
