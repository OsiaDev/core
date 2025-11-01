package co.cetad.umas.core.infrastructure.ugcs.adapter;

import co.cetad.umas.core.domain.model.vo.CommandRequest;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.DroneCache;
import co.cetad.umas.core.domain.ports.out.UgcsClient;
import co.cetad.umas.core.infrastructure.ugcs.listener.TelemetryNotificationListener;
import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.client.ClientSession;
import com.ugcs.ucs.proto.DomainProto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class UgcsClientAdapter implements UgcsClient {

    private final DroneCache droneCache;

    private Client client;
    private ClientSession session;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Sinks.Many<TelemetryData> telemetrySink = Sinks.many().multicast().onBackpressureBuffer();
    private int subscriptionId = -1;

    @Override
    public Mono<Void> connect(String host, int port, String username, String password) {
        return Mono.fromCallable(() -> {
                    log.info("Connecting to UgCS Server at {}:{}", host, port);

                    InetSocketAddress serverAddress = new InetSocketAddress(host, port);
                    client = new Client(serverAddress);

                    var listener = new TelemetryNotificationListener(telemetrySink, droneCache);
                    client.addNotificationListener(listener);

                    client.connect();

                    session = new ClientSession(client);
                    session.authorizeHci();
                    session.login(username, password);

                    subscriptionId = session.subscribeTelemetryEvent();

                    connected.set(true);
                    log.info("Successfully connected to UgCS Server");
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then()
                .doOnError(e -> {
                    log.error("Failed to connect to UgCS Server", e);
                    connected.set(false);
                });
    }

    @Override
    public Mono<Void> disconnect() {
        return Mono.fromCallable(() -> {
                    if (subscriptionId != -1 && session != null) {
                        session.unsubscribe(subscriptionId);
                    }
                    if (client != null) {
                        client.close();
                    }
                    connected.set(false);
                    telemetrySink.tryEmitComplete();
                    log.info("Disconnected from UgCS Server");
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Flux<TelemetryData> subscribeTelemetry() {
        return telemetrySink.asFlux()
                .doOnSubscribe(s -> log.info("Telemetry subscription started"))
                .doOnCancel(() -> log.info("Telemetry subscription cancelled"));
    }

    @Override
    public CompletableFuture<Boolean> executeCommand(CommandRequest command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                log.info("Executing command: {} for vehicle: {}",
                        command.commandCode(), command.vehicleId());

                var vehicle = findVehicle(command.vehicleId());
                if (vehicle == null) {
                    log.error("Vehicle not found: {}", command.vehicleId());
                    throw new IllegalArgumentException("Vehicle not found: " + command.vehicleId());
                }

                var ugcsCommand = buildCommand(command);

                log.debug("Gaining vehicle control for: {}", command.vehicleId());
                session.gainVehicleControl(vehicle);

                try {
                    log.debug("Sending command to vehicle");
                    session.sendCommand(vehicle, ugcsCommand);
                    log.info("Command sent successfully to vehicle: {}", command.vehicleId());
                    return true;
                } finally {
                    try {
                        session.releaseVehicleControl(vehicle);
                        log.debug("Vehicle control released for: {}", command.vehicleId());
                    } catch (Exception e) {
                        log.warn("Failed to release vehicle control", e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to execute command", e);
                throw new RuntimeException("Command execution failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isConnected() {
        return CompletableFuture.completedFuture(connected.get());
    }

    private DomainProto.Vehicle findVehicle(String vehicleId) {
        try {
            return session.lookupVehicle(vehicleId);
        } catch (Exception e) {
            log.error("Error looking up vehicle: {}", vehicleId, e);
            return null;
        }
    }

    private DomainProto.Command buildCommand(CommandRequest command) {
        var builder = DomainProto.Command.newBuilder()
                .setCode(command.commandCode())
                .setSubsystem(DomainProto.Subsystem.S_FLIGHT_CONTROLLER)
                .setSubsystemId(0);

        command.arguments().forEach((key, value) ->
                builder.addArguments(DomainProto.CommandArgument.newBuilder()
                        .setCode(key)
                        .setValue(DomainProto.Value.newBuilder()
                                .setDoubleValue(value)))
        );

        return builder.build();
    }

}