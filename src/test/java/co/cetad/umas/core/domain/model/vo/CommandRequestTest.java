package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandRequest Tests")
class CommandRequestTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create CommandRequest with all parameters")
        void shouldCreateCommandRequestWithAllParameters() {
            var arguments = Map.of("altitude", 100.0, "speed", 10.0);
            var request = new CommandRequest("vehicle-1", "takeoff_command", arguments);

            assertEquals("vehicle-1", request.vehicleId());
            assertEquals("takeoff_command", request.commandCode());
            assertEquals(arguments, request.arguments());
        }

        @Test
        @DisplayName("Should create CommandRequest with empty arguments")
        void shouldCreateCommandRequestWithEmptyArguments() {
            var request = new CommandRequest("vehicle-1", "arm", Map.of());

            assertEquals("vehicle-1", request.vehicleId());
            assertEquals("arm", request.commandCode());
            assertTrue(request.arguments().isEmpty());
        }
    }

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create simple command request")
        void shouldCreateSimpleCommandRequest() {
            var request = CommandRequest.simple("vehicle-1", "arm");

            assertEquals("vehicle-1", request.vehicleId());
            assertEquals("arm", request.commandCode());
            assertTrue(request.arguments().isEmpty());
        }

        @Test
        @DisplayName("Should create waypoint command request")
        void shouldCreateWaypointCommandRequest() {
            var request = CommandRequest.waypoint(
                    "vehicle-1", 45.0, -73.0, 100.0, 10.0
            );

            assertEquals("vehicle-1", request.vehicleId());
            assertEquals("waypoint", request.commandCode());
            assertEquals(45.0, request.arguments().get("latitude"));
            assertEquals(-73.0, request.arguments().get("longitude"));
            assertEquals(100.0, request.arguments().get("altitude_agl"));
            assertEquals(10.0, request.arguments().get("ground_speed"));
        }
    }
}
