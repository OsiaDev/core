package co.cetad.umas.core.domain.ports.out;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface UgcsClient {

    Mono<Void> connect(String host, int port, String username, String password);

    Mono<Void> disconnect();

    Flux<TelemetryData> subscribeTelemetry();

    CompletableFuture<Boolean> executeCommand(CommandRequest command);

    CompletableFuture<Boolean> isConnected();

}
