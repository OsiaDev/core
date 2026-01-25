package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RouteExecutionResult Tests")
class RouteExecutionResultTest {

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create in progress result")
        void shouldCreateInProgressResult() {
            var result = RouteExecutionResult.inProgress(
                    "vehicle-1", "route-1", 3, 10, "waypoint_4"
            );

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("route-1", result.routeId());
            assertEquals(RouteExecutionResult.RouteStatus.IN_PROGRESS, result.status());
            assertEquals(3, result.completedCommands());
            assertEquals(10, result.totalCommands());
            assertEquals("waypoint_4", result.currentCommand());
            assertTrue(result.message().contains("4 of 10"));
            assertTrue(result.errors().isEmpty());
            assertNotNull(result.startTime());
            assertNull(result.endTime());
        }

        @Test
        @DisplayName("Should create completed result")
        void shouldCreateCompletedResult() {
            var startTime = Instant.now().minusSeconds(60);

            var result = RouteExecutionResult.completed(
                    "vehicle-1", "route-1", 10, startTime
            );

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("route-1", result.routeId());
            assertEquals(RouteExecutionResult.RouteStatus.COMPLETED, result.status());
            assertEquals(10, result.completedCommands());
            assertEquals(10, result.totalCommands());
            assertNull(result.currentCommand());
            assertEquals("Route execution completed successfully", result.message());
            assertTrue(result.errors().isEmpty());
            assertEquals(startTime, result.startTime());
            assertNotNull(result.endTime());
        }

        @Test
        @DisplayName("Should create failed result")
        void shouldCreateFailedResult() {
            var startTime = Instant.now().minusSeconds(30);

            var result = RouteExecutionResult.failed(
                    "vehicle-1", "route-1", 5, 10,
                    "waypoint_6", "Connection lost", startTime
            );

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("route-1", result.routeId());
            assertEquals(RouteExecutionResult.RouteStatus.FAILED, result.status());
            assertEquals(5, result.completedCommands());
            assertEquals(10, result.totalCommands());
            assertEquals("waypoint_6", result.currentCommand());
            assertTrue(result.message().contains("failed at command 6"));
            assertEquals(List.of("Connection lost"), result.errors());
            assertEquals(startTime, result.startTime());
            assertNotNull(result.endTime());
        }
    }

    @Nested
    @DisplayName("RouteStatus enum tests")
    class RouteStatusEnumTests {

        @Test
        @DisplayName("Should have all expected status values")
        void shouldHaveAllExpectedStatusValues() {
            var statuses = RouteExecutionResult.RouteStatus.values();

            assertEquals(5, statuses.length);
            assertNotNull(RouteExecutionResult.RouteStatus.IN_PROGRESS);
            assertNotNull(RouteExecutionResult.RouteStatus.COMPLETED);
            assertNotNull(RouteExecutionResult.RouteStatus.FAILED);
            assertNotNull(RouteExecutionResult.RouteStatus.CANCELLED);
            assertNotNull(RouteExecutionResult.RouteStatus.TIMEOUT);
        }
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create result with all parameters")
        void shouldCreateResultWithAllParameters() {
            var startTime = Instant.now().minusSeconds(60);
            var endTime = Instant.now();

            var result = new RouteExecutionResult(
                    "vehicle-1",
                    "route-1",
                    RouteExecutionResult.RouteStatus.CANCELLED,
                    7,
                    10,
                    "waypoint_8",
                    "User cancelled",
                    List.of("User requested cancellation"),
                    startTime,
                    endTime
            );

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("route-1", result.routeId());
            assertEquals(RouteExecutionResult.RouteStatus.CANCELLED, result.status());
            assertEquals(7, result.completedCommands());
            assertEquals(10, result.totalCommands());
            assertEquals("waypoint_8", result.currentCommand());
            assertEquals("User cancelled", result.message());
            assertEquals(1, result.errors().size());
            assertEquals(startTime, result.startTime());
            assertEquals(endTime, result.endTime());
        }
    }
}
