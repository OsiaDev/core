package co.cetad.umas.core.application.service.command;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class CommandValidator {

    // Comandos básicos de control
    private static final Set<String> VALID_COMMANDS = Set.of(
            // Control básico
            "arm", "disarm", "auto", "manual", "guided", "joystick",

            // Comandos de vuelo
            "takeoff_command", "land_command", "emergency_land",
            "return_to_home",

            // Control de misión/ruta
            "mission_pause", "mission_resume",
            "start_route", "pause_route", "resume_route", "stop_route",

            // Waypoint y control directo
            "waypoint", "direct_vehicle_control"
    );

    private static final Set<String> WAYPOINT_ARGS = Set.of(
            "latitude", "longitude", "altitude_amsl", "altitude_agl",
            "altitude_origin", "ground_speed", "vertical_speed",
            "acceptance_radius", "heading"
    );

    private static final Set<String> CONTROL_ARGS = Set.of(
            "pitch", "roll", "yaw", "throttle"
    );

    // Comandos que no requieren argumentos
    private static final Set<String> NO_ARG_COMMANDS = Set.of(
            "arm", "disarm", "auto", "manual", "guided",
            "emergency_land", "return_to_home",
            "mission_pause", "mission_resume",
            "start_route", "pause_route", "resume_route", "stop_route"
    );

    public CompletableFuture<Void> validate(CommandExecutionDTO command) {
        return CompletableFuture.runAsync(() -> {
            validateCommandCode(command.commandCode());
            validateArguments(command);
        });
    }

    private void validateCommandCode(String commandCode) {
        if (!VALID_COMMANDS.contains(commandCode)) {
            throw new IllegalArgumentException("Invalid command code: " + commandCode);
        }
    }

    private void validateArguments(CommandExecutionDTO command) {
        String commandCode = command.commandCode();

        // Comandos que no deberían tener argumentos
        if (NO_ARG_COMMANDS.contains(commandCode)) {
            if (command.arguments() != null && !command.arguments().isEmpty()) {
                log.warn("Command '{}' does not require arguments, but {} were provided",
                        commandCode, command.arguments().size());
            }
            return;
        }

        // Comandos que requieren argumentos específicos
        switch (commandCode) {
            case "waypoint" -> validateWaypointCommand(command);
            case "direct_vehicle_control" -> validateControlCommand(command);
            case "takeoff_command", "land_command" -> validateAltitudeCommand(command);
        }
    }

    private void validateWaypointCommand(CommandExecutionDTO command) {
        var args = command.arguments();

        if (args == null || args.isEmpty()) {
            throw new IllegalArgumentException(
                    "Waypoint command requires arguments");
        }

        if (!args.containsKey("latitude") || !args.containsKey("longitude")) {
            throw new IllegalArgumentException(
                    "Waypoint command requires latitude and longitude");
        }

        validateCoordinates(args);
        warnUnknownArguments(args, WAYPOINT_ARGS);
    }

    private void validateCoordinates(Map<String, Double> args) {
        var lat = args.get("latitude");
        var lon = args.get("longitude");

        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Invalid latitude: " + lat);
        }

        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Invalid longitude: " + lon);
        }
    }

    private void validateControlCommand(CommandExecutionDTO command) {
        var args = command.arguments();
        if (args != null && !args.isEmpty()) {
            warnUnknownArguments(args, CONTROL_ARGS);
        }
    }

    private void validateAltitudeCommand(CommandExecutionDTO command) {
        var args = command.arguments();
        if (args != null && args.containsKey("altitude")) {
            double altitude = args.get("altitude");
            if (altitude < 0) {
                throw new IllegalArgumentException("Altitude cannot be negative: " + altitude);
            }
        }
    }

    private void warnUnknownArguments(Map<String, Double> args, Set<String> validArgs) {
        args.keySet().forEach(key -> {
            if (!validArgs.contains(key)) {
                log.warn("Unknown argument: {}", key);
            }
        });
    }
}