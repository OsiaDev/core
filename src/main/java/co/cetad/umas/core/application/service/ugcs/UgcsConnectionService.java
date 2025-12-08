package co.cetad.umas.core.application.service.ugcs;

import co.cetad.umas.core.application.service.mission.MissionCompleteProcessorService;
import co.cetad.umas.core.application.service.telemetry.TelemetryProcessorService;
import co.cetad.umas.core.domain.model.dto.VehicleStatusDTO;
import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import co.cetad.umas.core.domain.ports.out.StatusNotifier;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import co.cetad.umas.core.infrastructure.ugcs.config.UgcsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UgcsConnectionService implements VehicleConnectionManager {

    private final UgcsClient ugcsClient;
    private final StatusNotifier statusNotifier;
    private final TelemetryProcessorService telemetryProcessorService;
    private final MissionCompleteProcessorService missionCompleteProcessorService;
    private final UgcsProperties properties;

    @Override
    public Mono<Void> connect() {
        log.info("🔌 Connecting to UgCS Server at {}:{}",
                properties.getServer().getHost(),
                properties.getServer().getPort());

        return ugcsClient.connect(
                        properties.getServer().getHost(),
                        properties.getServer().getPort(),
                        properties.getCredentials().getLogin(),
                        properties.getCredentials().getPassword()
                )
                .retryWhen(buildRetrySpec())
                .doOnSuccess(v -> {
                    log.info("✅ Successfully connected to UgCS Server");
                })
                .doOnError(e -> {
                    log.error("❌ Failed to connect to UgCS Server", e);
                });
    }

    @Override
    public Mono<Void> disconnect() {
        log.info("Disconnecting from UgCS Server");

        return ugcsClient.disconnect()
                .doOnSuccess(v -> {
                    log.info("Disconnected from UgCS Server");
                })
                .doOnError(e -> log.error("Error during disconnect", e));
    }

    @Override
    public CompletableFuture<Boolean> isConnected() {
        return ugcsClient.isConnected();
    }

    @Override
    public Mono<Void> subscribeTelemetry() {
        log.info("🎧 Starting telemetry subscription");

        return ugcsClient.subscribeTelemetry()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(telemetry -> {
                    log.trace("Received telemetry for vehicle: {}", telemetry.vehicleId());

                    // Procesar telemetría de forma asíncrona
                    telemetryProcessorService.process(telemetry)
                            .exceptionally(error -> {
                                log.error("Error processing telemetry for vehicle: {}",
                                        telemetry.vehicleId(), error);
                                return null;
                            });
                })
                .doOnError(error -> log.error("Error in telemetry subscription", error))
                .retry()  // Reintentar automáticamente en caso de error
                .then();
    }

    @Override
    public Mono<Void> subscribeMissionComplete() {
        log.info("🎧 Starting mission complete event subscription");

        return ugcsClient.subscribeMissionComplete()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(missionComplete -> {
                    log.info("📥 Received mission complete event for vehicle: {}",
                            missionComplete.vehicleId());

                    missionCompleteProcessorService.process(missionComplete)
                            .exceptionally(error -> {
                                log.error("❌ Error processing mission complete for vehicle: {}",
                                        missionComplete.vehicleId(), error);
                                return null;
                            });
                })
                .doOnError(error -> log.error("Error in telemetry subscription", error))
                .retry()  // Reintentar automáticamente en caso de error
                .then();
    }

    private Retry buildRetrySpec() {
        if (!properties.getReconnect().isEnabled()) {
            return Retry.max(0);
        }

        return Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(properties.getReconnect().getInitialDelay()))
                .maxBackoff(Duration.ofMillis(properties.getReconnect().getMaxDelay()))
                .doBeforeRetry(signal -> log.warn(
                        "Retrying connection to UgCS Server, attempt: {}",
                        signal.totalRetries() + 1
                ));
    }

    private void notifyStatus(VehicleStatusDTO status) {
        statusNotifier.notify(status)
                .doOnError(e -> log.error("Failed to notify status", e))
                .subscribe();
    }

}