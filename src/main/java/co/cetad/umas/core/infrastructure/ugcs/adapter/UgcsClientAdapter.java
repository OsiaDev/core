package co.cetad.umas.core.infrastructure.ugcs.adapter;

import co.cetad.umas.core.domain.model.dto.MissionExecutionDTO;
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
import java.util.List;
import java.util.Optional;
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
                    log.debug("Sending command '{}' to vehicle {}",
                            command.commandCode(), command.vehicleId());
                    session.sendCommand(vehicle, ugcsCommand);
                    log.info("Command '{}' sent successfully to vehicle: {}",
                            command.commandCode(), command.vehicleId());
                    return true;
                } finally {
                    try {
                        session.releaseVehicleControl(vehicle);
                        log.debug("Vehicle control released for: {}", command.vehicleId());
                    } catch (Exception e) {
                        log.warn("Failed to release vehicle control for vehicle: {}",
                                command.vehicleId(), e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to execute command '{}' for vehicle: {}",
                        command.commandCode(), command.vehicleId(), e);
                throw new RuntimeException("Command execution failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Object> findOrCreateMission(String missionName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                log.info("Finding or creating mission: '{}'", missionName);

                // Buscar misión existente
                List<DomainProto.DomainObjectWrapper> missions = session.getObjectList(DomainProto.Mission.class);
                for (DomainProto.DomainObjectWrapper wrapper : missions) {
                    if (wrapper.getMission().getName().equals(missionName)) {
                        log.info("✅ Found existing mission: '{}'", missionName);
                        return wrapper.getMission();
                    }
                }

                // Crear nueva misión
                log.info("Creating new mission: '{}'", missionName);
                DomainProto.User user = session.getObjectList(DomainProto.User.class).get(0).getUser();

                DomainProto.Mission newMission = DomainProto.Mission.newBuilder()
                        .setName(missionName)
                        .setOwner(user)
                        .build();

                DomainProto.DomainObjectWrapper missionWrapper = DomainProto.DomainObjectWrapper.newBuilder()
                        .setMission(newMission)
                        .build();

                newMission = session.createOrUpdateObject(missionWrapper, DomainProto.Mission.class).getMission();
                log.info("✅ Mission created: '{}'", missionName);

                return newMission;

            } catch (Exception e) {
                log.error("Failed to find or create mission: {}", missionName, e);
                throw new RuntimeException("Mission operation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<DomainProto.Route>> findRouteByName(String routeName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                log.info("Searching for route: '{}'", routeName);

                List<DomainProto.DomainObjectWrapper> routeWrappers = session.getObjectList(DomainProto.Route.class);

                if (routeWrappers == null || routeWrappers.isEmpty()) {
                    log.info("No routes found in UgCS Server");
                    return Optional.empty();
                }

                for (var wrapper : routeWrappers) {
                    var route = wrapper.getRoute();
                    if (route.getName().equals(routeName)) {
                        log.info("✅ Found existing route: '{}'", routeName);
                        return Optional.of(route);
                    }
                }

                log.info("Route '{}' not found", routeName);
                return Optional.empty();

            } catch (Exception e) {
                log.error("Failed to search for route: {}", routeName, e);
                throw new RuntimeException("Route search failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> createAndUploadRoute(
            Object ugcsMission,
            String vehicleId,
            String routeName,
            List<MissionExecutionDTO.SimpleWaypoint> waypoints,
            double altitude,
            double speed
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                if (!(ugcsMission instanceof DomainProto.Mission)) {
                    throw new IllegalArgumentException("ugcsMission must be of type DomainProto.Mission");
                }

                DomainProto.Mission mission = (DomainProto.Mission) ugcsMission;
                log.info("Creating and uploading route '{}' with {} waypoints to vehicle: {}",
                        routeName, waypoints.size(), vehicleId);

                // 1. Buscar el vehículo
                DomainProto.Vehicle vehicle = findVehicle(vehicleId);
                if (vehicle == null) {
                    throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
                }

                // 2. Construir la ruta
                DomainProto.Route route = buildRoute(mission, vehicle, routeName, waypoints, altitude, speed);

                // 3. Guardar la ruta en el servidor
                log.debug("Saving route to UgCS Server");
                DomainProto.DomainObjectWrapper routeWrapper = DomainProto.DomainObjectWrapper.newBuilder()
                        .setRoute(route)
                        .build();

                route = session.createOrUpdateObject(routeWrapper, DomainProto.Route.class).getRoute();
                log.info("✅ Route '{}' saved to server", route.getName());

                // 4. Subir la ruta al vehículo
                uploadRouteToVehicle(vehicle, route);

                return true;

            } catch (Exception e) {
                log.error("Failed to create and upload route '{}' for vehicle: {}",
                        routeName, vehicleId, e);
                throw new RuntimeException("Route creation failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> uploadExistingRoute(String vehicleId, Object existingRoute) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                if (!(existingRoute instanceof DomainProto.Route)) {
                    throw new IllegalArgumentException("existingRoute must be of type DomainProto.Route");
                }

                var route = (DomainProto.Route) existingRoute;
                log.info("Uploading existing route '{}' to vehicle: {}", route.getName(), vehicleId);

                var vehicle = findVehicle(vehicleId);
                if (vehicle == null) {
                    throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
                }

                uploadRouteToVehicle(vehicle, route);
                return true;

            } catch (Exception e) {
                log.error("Failed to upload existing route for vehicle: {}", vehicleId, e);
                throw new RuntimeException("Existing route upload failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<RouteInfo>> getVehicleRoutes(String vehicleId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!connected.get()) {
                    throw new IllegalStateException("Not connected to UgCS Server");
                }

                log.info("Getting all routes for vehicle: {}", vehicleId);

                var vehicle = findVehicle(vehicleId);
                if (vehicle == null) {
                    throw new IllegalArgumentException("Vehicle not found: " + vehicleId);
                }

                var routeWrappers = session.getObjectList(DomainProto.Route.class);
                if (routeWrappers == null || routeWrappers.isEmpty()) {
                    log.info("No routes found in UgCS Server");
                    return List.of();
                }

                return routeWrappers.stream()
                        .map(DomainProto.DomainObjectWrapper::getRoute)
                        .filter(route -> route.getVehicleProfile().equals(vehicle.getProfile()))
                        .map(route -> new UgcsClient.RouteInfo(
                                route.getName(),
                                route.getSegmentsCount(),
                                route
                        ))
                        .toList();

            } catch (Exception e) {
                log.error("Failed to get routes for vehicle: {}", vehicleId, e);
                throw new RuntimeException("Get routes failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isConnected() {
        return CompletableFuture.completedFuture(connected.get());
    }

    // ==================== MÉTODOS PRIVADOS HELPER ====================

    private DomainProto.Vehicle findVehicle(String vehicleId) {
        try {
            var vehicle = session.lookupVehicle(vehicleId);
            if (vehicle == null) {
                log.warn("Vehicle lookup returned null for ID: {}", vehicleId);
            }
            return vehicle;
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

        if (command.arguments() != null && !command.arguments().isEmpty()) {
            command.arguments().forEach((key, value) ->
                    builder.addArguments(DomainProto.CommandArgument.newBuilder()
                            .setCode(key)
                            .setValue(DomainProto.Value.newBuilder()
                                    .setDoubleValue(value)
                                    .build())
                            .build())
            );
        }

        return builder.build();
    }

    /**
     * Construye una ruta de UgCS basada en waypoints simples
     */
    private DomainProto.Route buildRoute(
            DomainProto.Mission mission,
            DomainProto.Vehicle vehicle,
            String routeName,
            List<MissionExecutionDTO.SimpleWaypoint> waypoints,
            double altitude,
            double speed
    ) {
        log.debug("Building route: '{}' with {} waypoints", routeName, waypoints.size());

        // Construir figura con todos los waypoints
        DomainProto.Figure.Builder figure = DomainProto.Figure.newBuilder()
                .setType(DomainProto.FigureType.FT_POINT);

        for (int i = 0; i < waypoints.size(); i++) {
            var wp = waypoints.get(i);

            // IMPORTANTE: UgCS requiere lat/lon en RADIANES, no en grados
            double latRadians = Math.toRadians(wp.latitude());
            double lonRadians = Math.toRadians(wp.longitude());

            figure.addPoints(DomainProto.FigurePoint.newBuilder()
                    .setLatitude(latRadians)
                    .setLongitude(lonRadians)
                    .setAglAltitude(altitude)
                    .setAltitudeType(DomainProto.AltitudeType.AT_AGL));
        }

        // Construir segmento de ruta
        DomainProto.SegmentDefinition.Builder routeSegment = DomainProto.SegmentDefinition.newBuilder()
                .setAlgorithmClassName("com.ugcs.ucs.service.routing.impl.WaypointAlgorithm")
                .setFigure(figure)
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("speed")
                        .setValue(Double.toString(speed)))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("wpTurnType")
                        .setValue("STOP_AND_TURN"))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("avoidObstacles")
                        .setValue("true"))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("avoidTerrain")
                        .setValue("true"))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("cornerRadius"))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("altitudeType")
                        .setValue("AGL"))
                .addParameterValues(DomainProto.ParameterValue.newBuilder()
                        .setName("acceptanceRadius")
                        .setValue("5.0"));  // Radio de aceptación en metros

        // Construir ruta completa
        DomainProto.Route.Builder route = DomainProto.Route.newBuilder()
                .setMission(mission)
                .setName(routeName)
                .setCheckAerodromeNfz(true)
                .setCheckCustomNfz(false)
                .setInitialSpeed(speed)
                .setMaxSpeed(25.0)
                .setMaxAltitude(10000.0)
                .setSafeAltitude(altitude)
                .addFailsafes(DomainProto.Failsafe.newBuilder()
                        .setReason(DomainProto.FailsafeReason.FR_GPS_LOST)
                        .setAction(DomainProto.FailsafeAction.FA_WAIT))
                .addSegments(routeSegment);

        if (vehicle != null) {
            route.setVehicleProfile(vehicle.getProfile());
        }

        log.debug("✅ Route built: '{}'", routeName);
        return route.build();
    }

    /**
     * Procesa y sube una ruta al vehículo
     */
    private void uploadRouteToVehicle(DomainProto.Vehicle vehicle, DomainProto.Route route) {
        try {
            log.debug("Processing route trajectory for: {}", route.getName());
            DomainProto.ProcessedRoute processedRoute = session.processRoute(route);

            log.debug("Gaining vehicle control for route upload: {}", vehicle.getName());
            session.gainVehicleControl(vehicle);

            try {
                log.debug("Uploading processed route to vehicle: {}", vehicle.getName());
                session.uploadRoute(vehicle, processedRoute);
                log.info("✅ Route '{}' uploaded successfully to vehicle: {}",
                        route.getName(), vehicle.getName());
            } finally {
                try {
                    session.releaseVehicleControl(vehicle);
                    log.debug("Vehicle control released after route upload");
                } catch (Exception e) {
                    log.warn("Failed to release vehicle control", e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to upload route to vehicle", e);
            throw new RuntimeException("Route upload to vehicle failed: " + e.getMessage(), e);
        }
    }
}