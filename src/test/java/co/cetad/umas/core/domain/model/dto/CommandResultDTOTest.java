package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandResultDTO Tests")
class CommandResultDTOTest {

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create success result")
        void shouldCreateSuccessResult() {
            var result = CommandResultDTO.success("vehicle-1", "arm");

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("arm", result.commandCode());
            assertEquals(CommandResultDTO.CommandStatus.SUCCESS, result.status());
            assertEquals("Command executed successfully", result.message());
            assertNotNull(result.timestamp());
        }

        @Test
        @DisplayName("Should create failed result")
        void shouldCreateFailedResult() {
            var result = CommandResultDTO.failed("vehicle-1", "arm", "Connection lost");

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("arm", result.commandCode());
            assertEquals(CommandResultDTO.CommandStatus.FAILED, result.status());
            assertEquals("Connection lost", result.message());
            assertNotNull(result.timestamp());
        }
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create DTO with all parameters")
        void shouldCreateDtoWithAllParameters() {
            var now = Instant.now();
            var result = new CommandResultDTO(
                    "vehicle-1",
                    "takeoff_command",
                    CommandResultDTO.CommandStatus.TIMEOUT,
                    "Command timed out",
                    now
            );

            assertEquals("vehicle-1", result.vehicleId());
            assertEquals("takeoff_command", result.commandCode());
            assertEquals(CommandResultDTO.CommandStatus.TIMEOUT, result.status());
            assertEquals("Command timed out", result.message());
            assertEquals(now, result.timestamp());
        }
    }

    @Nested
    @DisplayName("CommandStatus enum tests")
    class CommandStatusEnumTests {

        @Test
        @DisplayName("Should have all expected status values")
        void shouldHaveAllExpectedStatusValues() {
            var statuses = CommandResultDTO.CommandStatus.values();

            assertEquals(4, statuses.length);
            assertNotNull(CommandResultDTO.CommandStatus.SUCCESS);
            assertNotNull(CommandResultDTO.CommandStatus.FAILED);
            assertNotNull(CommandResultDTO.CommandStatus.REJECTED);
            assertNotNull(CommandResultDTO.CommandStatus.TIMEOUT);
        }

        @Test
        @DisplayName("Should parse status from string")
        void shouldParseStatusFromString() {
            assertEquals(CommandResultDTO.CommandStatus.SUCCESS,
                    CommandResultDTO.CommandStatus.valueOf("SUCCESS"));
            assertEquals(CommandResultDTO.CommandStatus.FAILED,
                    CommandResultDTO.CommandStatus.valueOf("FAILED"));
        }
    }
}
