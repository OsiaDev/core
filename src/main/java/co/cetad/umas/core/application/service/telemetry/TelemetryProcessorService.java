package co.cetad.umas.core.application.service.telemetry;

import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.in.EventProcessor;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryProcessorService implements EventProcessor<TelemetryData, Void> {

    private final EventPublisher<TelemetryData> telemetryPublisher;

    @Override
    public CompletableFuture<Void> process(TelemetryData event) {
        log.debug("Processing telemetry for vehicle: {}", event.vehicleId());

        return telemetryPublisher.publish(event)
                .doOnSuccess(v -> log.trace("Telemetry published successfully"))
                .doOnError(e -> log.error("Failed to process telemetry", e))
                .toFuture()
                .thenApply(v -> null);
    }

}
