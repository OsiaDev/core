package co.cetad.umas.core.domain.ports.in;

import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

public interface VehicleConnectionManager {

    Mono<Void> connect();

    Mono<Void> disconnect();

    CompletableFuture<Boolean> isConnected();

    Mono<Void> subscribeTelemetry();

}
