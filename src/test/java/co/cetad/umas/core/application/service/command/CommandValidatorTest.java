package co.cetad.umas.core.application.service.command;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandValidator Tests")
class CommandValidatorTest {

    private CommandValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CommandValidator();
    }

    @Nested
    @DisplayName("Valid commands tests")
    class ValidCommandsTests {

        @Test
        @DisplayName("Should accept arm command without arguments")
        void shouldAcceptArmCommandWithoutArguments() {
            var command = createCommand("arm", null);

            assertDoesNotThrow(() -> validator.validate(command).get());
        }
    }

    @Nested
    @DisplayName("Invalid commands tests")
    class InvalidCommandsTests {

        @Test
        @DisplayName("Should reject invalid command code")
        void shouldRejectInvalidCommandCode() {
            var command = createCommand("invalid_command", Map.of());

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("Invalid command code"));
        }
    }

    @Nested
    @DisplayName("Waypoint command validation tests")
    class WaypointCommandValidationTests {

        @Test
        @DisplayName("Should accept valid waypoint command")
        void shouldAcceptValidWaypointCommand() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", 45.0,
                    "longitude", -73.0,
                    "altitude_agl", 100.0
            ));

            assertDoesNotThrow(() -> validator.validate(command).get());
        }

        @Test
        @DisplayName("Should reject waypoint command without arguments")
        void shouldRejectWaypointCommandWithoutArguments() {
            var command = createCommand("waypoint", Map.of());

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("requires arguments"));
        }

        @Test
        @DisplayName("Should reject waypoint command without latitude")
        void shouldRejectWaypointCommandWithoutLatitude() {
            var command = createCommand("waypoint", Map.of("longitude", -73.0));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("latitude and longitude"));
        }

        @Test
        @DisplayName("Should reject waypoint command without longitude")
        void shouldRejectWaypointCommandWithoutLongitude() {
            var command = createCommand("waypoint", Map.of("latitude", 45.0));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("latitude and longitude"));
        }

        @Test
        @DisplayName("Should reject waypoint command with invalid latitude")
        void shouldRejectWaypointCommandWithInvalidLatitude() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", 91.0,
                    "longitude", -73.0
            ));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("Invalid latitude"));
        }

        @Test
        @DisplayName("Should reject waypoint command with latitude below range")
        void shouldRejectWaypointCommandWithLatitudeBelowRange() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", -91.0,
                    "longitude", -73.0
            ));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }

        @Test
        @DisplayName("Should reject waypoint command with invalid longitude")
        void shouldRejectWaypointCommandWithInvalidLongitude() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", 45.0,
                    "longitude", 181.0
            ));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("Invalid longitude"));
        }

        @Test
        @DisplayName("Should reject waypoint command with longitude below range")
        void shouldRejectWaypointCommandWithLongitudeBelowRange() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", 45.0,
                    "longitude", -181.0
            ));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }

        @Test
        @DisplayName("Should accept waypoint command with valid waypoint arguments")
        void shouldAcceptWaypointCommandWithValidWaypointArguments() {
            var command = createCommand("waypoint", Map.of(
                    "latitude", 45.0,
                    "longitude", -73.0,
                    "altitude_amsl", 100.0,
                    "ground_speed", 10.0,
                    "heading", 90.0
            ));

            assertDoesNotThrow(() -> validator.validate(command).get());
        }
    }

    @Nested
    @DisplayName("Direct vehicle control validation tests")
    class DirectVehicleControlValidationTests {

        @Test
        @DisplayName("Should accept direct_vehicle_control without arguments")
        void shouldAcceptDirectVehicleControlWithoutArguments() {
            var command = createCommand("direct_vehicle_control", Map.of());

            assertDoesNotThrow(() -> validator.validate(command).get());
        }

        @Test
        @DisplayName("Should accept direct_vehicle_control with valid control arguments")
        void shouldAcceptDirectVehicleControlWithValidControlArguments() {
            var command = createCommand("direct_vehicle_control", Map.of(
                    "pitch", 0.5,
                    "roll", -0.3,
                    "yaw", 0.0,
                    "throttle", 0.8
            ));

            assertDoesNotThrow(() -> validator.validate(command).get());
        }
    }

    @Nested
    @DisplayName("Altitude command validation tests")
    class AltitudeCommandValidationTests {

        @Test
        @DisplayName("Should accept takeoff_command with positive altitude")
        void shouldAcceptTakeoffCommandWithPositiveAltitude() {
            var command = createCommand("takeoff_command", Map.of("altitude", 50.0));

            assertDoesNotThrow(() -> validator.validate(command).get());
        }

        @Test
        @DisplayName("Should accept takeoff_command without altitude")
        void shouldAcceptTakeoffCommandWithoutAltitude() {
            var command = createCommand("takeoff_command", Map.of());

            assertDoesNotThrow(() -> validator.validate(command).get());
        }

        @Test
        @DisplayName("Should reject takeoff_command with negative altitude")
        void shouldRejectTakeoffCommandWithNegativeAltitude() {
            var command = createCommand("takeoff_command", Map.of("altitude", -10.0));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
            assertTrue(exception.getCause().getMessage().contains("Altitude cannot be negative"));
        }

        @Test
        @DisplayName("Should accept land_command with positive altitude")
        void shouldAcceptLandCommandWithPositiveAltitude() {
            var command = createCommand("land_command", Map.of("altitude", 0.0));

            assertDoesNotThrow(() -> validator.validate(command).get());
        }

        @Test
        @DisplayName("Should reject land_command with negative altitude")
        void shouldRejectLandCommandWithNegativeAltitude() {
            var command = createCommand("land_command", Map.of("altitude", -5.0));

            var exception = assertThrows(ExecutionException.class,
                    () -> validator.validate(command).get());
            assertTrue(exception.getCause() instanceof IllegalArgumentException);
        }
    }

    @Nested
    @DisplayName("No argument commands tests")
    class NoArgumentCommandsTests {

        @ParameterizedTest
        @ValueSource(strings = {"arm", "disarm", "auto", "manual", "guided",
                "emergency_land", "return_to_home", "mission_pause", "mission_resume",
                "start_route", "pause_route", "resume_route", "stop_route"})
        @DisplayName("Should accept no-arg commands with extra arguments (warn only)")
        void shouldAcceptNoArgCommandsWithExtraArguments(String commandCode) {
            var command = createCommand(commandCode, Map.of("extra", 1.0));

            // Should not throw, just log a warning
            assertDoesNotThrow(() -> validator.validate(command).get());
        }
    }

    private CommandExecutionDTO createCommand(String commandCode, Map<String, Double> arguments) {
        return new CommandExecutionDTO("vehicle-1", "mission-1", commandCode, arguments, 1);
    }
}
