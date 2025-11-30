package co.cetad.umas.core.application.service.route;

import co.cetad.umas.core.domain.model.dto.RouteUploadDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class RouteValidator {

    private static final int MIN_WAYPOINTS = 2;
    private static final int MAX_WAYPOINTS = 200;
    private static final double MIN_ALTITUDE = 0.0;
    private static final double MAX_ALTITUDE = 500.0;  // metros AGL
    private static final double MIN_SPEED = 0.5;       // m/s
    private static final double MAX_SPEED = 25.0;      // m/s

    public CompletableFuture<Void> validate(RouteUploadDTO route) {
        return CompletableFuture.runAsync(() -> {
            validateBasicInfo(route);
            validateWaypoints(route);
            validateSettings(route);
        });
    }

    private void validateBasicInfo(RouteUploadDTO route) {
        if (route.vehicleId() == null || route.vehicleId().isBlank()) {
            throw new IllegalArgumentException("Vehicle ID is required");
        }

        if (route.routeName() == null || route.routeName().isBlank()) {
            throw new IllegalArgumentException("Route name is required");
        }

        if (route.waypoints() == null || route.waypoints().isEmpty()) {
            throw new IllegalArgumentException("Route must contain at least one waypoint");
        }
    }

    private void validateWaypoints(RouteUploadDTO route) {
        var waypoints = route.waypoints();

        if (waypoints.size() < MIN_WAYPOINTS) {
            throw new IllegalArgumentException(
                    String.format("Route must have at least %d waypoints, got %d",
                            MIN_WAYPOINTS, waypoints.size()));
        }

        if (waypoints.size() > MAX_WAYPOINTS) {
            log.warn("Route has {} waypoints, which exceeds recommended limit of {}",
                    waypoints.size(), MAX_WAYPOINTS);
        }

        for (int i = 0; i < waypoints.size(); i++) {
            validateWaypoint(waypoints.get(i), i);
        }
    }

    private void validateWaypoint(RouteUploadDTO.WaypointDTO waypoint, int index) {
        // Validar coordenadas
        if (waypoint.latitude() < -90 || waypoint.latitude() > 90) {
            throw new IllegalArgumentException(
                    String.format("Invalid latitude at waypoint %d: %f", index, waypoint.latitude()));
        }

        if (waypoint.longitude() < -180 || waypoint.longitude() > 180) {
            throw new IllegalArgumentException(
                    String.format("Invalid longitude at waypoint %d: %f", index, waypoint.longitude()));
        }

        // Validar altitud
        if (waypoint.altitude() < MIN_ALTITUDE) {
            throw new IllegalArgumentException(
                    String.format("Altitude at waypoint %d cannot be negative: %f",
                            index, waypoint.altitude()));
        }

        if (waypoint.altitude() > MAX_ALTITUDE) {
            log.warn("Waypoint {} altitude ({} m) exceeds recommended maximum of {} m",
                    index, waypoint.altitude(), MAX_ALTITUDE);
        }

        // Validar velocidad si está presente
        if (waypoint.speed() != null) {
            if (waypoint.speed() < MIN_SPEED || waypoint.speed() > MAX_SPEED) {
                throw new IllegalArgumentException(
                        String.format("Speed at waypoint %d must be between %f and %f m/s, got %f",
                                index, MIN_SPEED, MAX_SPEED, waypoint.speed()));
            }
        }

        // Validar heading si está presente
        if (waypoint.heading() != null) {
            if (waypoint.heading() < 0 || waypoint.heading() > 360) {
                throw new IllegalArgumentException(
                        String.format("Heading at waypoint %d must be between 0 and 360 degrees, got %f",
                                index, waypoint.heading()));
            }
        }

        // Validar acceptance radius si está presente
        if (waypoint.acceptanceRadius() != null) {
            if (waypoint.acceptanceRadius() <= 0) {
                throw new IllegalArgumentException(
                        String.format("Acceptance radius at waypoint %d must be positive, got %f",
                                index, waypoint.acceptanceRadius()));
            }
        }
    }

    private void validateSettings(RouteUploadDTO route) {
        if (route.settings() == null) {
            return; // Settings son opcionales, se usarán defaults
        }

        var settings = route.settings();

        if (settings.defaultSpeed() != null) {
            if (settings.defaultSpeed() < MIN_SPEED || settings.defaultSpeed() > MAX_SPEED) {
                throw new IllegalArgumentException(
                        String.format("Default speed must be between %f and %f m/s, got %f",
                                MIN_SPEED, MAX_SPEED, settings.defaultSpeed()));
            }
        }

        if (settings.defaultAltitude() != null) {
            if (settings.defaultAltitude() < MIN_ALTITUDE) {
                throw new IllegalArgumentException("Default altitude cannot be negative");
            }
        }

        if (settings.defaultAcceptanceRadius() != null) {
            if (settings.defaultAcceptanceRadius() <= 0) {
                throw new IllegalArgumentException("Default acceptance radius must be positive");
            }
        }
    }
}