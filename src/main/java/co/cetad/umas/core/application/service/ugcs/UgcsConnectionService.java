package co.cetad.umas.core.application.service.ugcs;

import co.cetad.umas.core.domain.model.dto.VehicleStatusDTO;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.in.VehicleConnectionManager;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
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
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class UgcsConnectionService implements VehicleConnectionManager {

    private final UgcsClient ugcsClient;
    private final UgcsProperties properties;
    private final StatusNotifier statusNotifier;
    private final EventPublisher<TelemetryData> telemetryPublisher;

    @Override
    public Mono<Void> connect() {
        return ugcsClient.connect(
                        properties.getServer().getHost(),
                        properties.getServer().getPort(),
                        properties.getCredentials().getLogin(),
                        properties.getCredentials().getPassword()
                )
                .retryWhen(buildRetrySpec())
                .doOnSuccess(v -> {
                    log.info("Successfully connected to UgCS Server");
                    notifyStatus(VehicleStatusDTO.connected("system"));
                    disconnect();
                })
                .doOnError(e -> {
                    log.error("Failed to connect after retries", e);
                    notifyStatus(VehicleStatusDTO.error("system", e.getMessage()));
                })
                .then();
    }

    @Override
    public Mono<Void> disconnect() {
        return ugcsClient.disconnect()
                .doOnSuccess(v -> {
                    log.info("Disconnected from UgCS Server");
                    notifyStatus(VehicleStatusDTO.error("system", "Disconnected"));
                });
    }

    @Override
    public CompletableFuture<Boolean> isConnected() {
        return ugcsClient.isConnected();
    }

    @Override
    public Mono<Void> subscribeTelemetry() {
        return ugcsClient.subscribeTelemetry()
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(telemetry -> {
                    /*log.debug("Received telemetry for vehicle: {} at ({}, {})",
                            telemetry.vehicleId(),
                            telemetry.location().latitude(),
                            telemetry.location().longitude());*/

                    telemetryPublisher.publish(telemetry)
                            .doOnError(e -> log.error("Failed to publish telemetry", e))
                            .subscribe();
                })
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