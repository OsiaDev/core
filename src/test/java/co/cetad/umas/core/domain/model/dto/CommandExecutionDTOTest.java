package co.cetad.umas.core.domain.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandExecutionDTO Tests")
class CommandExecutionDTOTest {

    @Nested
    @DisplayName("Constructor validation tests")
    class ConstructorValidationTests {

        @Test
        @DisplayName("Should create DTO with valid parameters")
        void shouldCreateDtoWithValidParameters() {
            var dto = new CommandExecutionDTO(
                    "vehicle-1",
                    "mission-1",
                    "arm",
                    Map.of("altitude", 100.0),
                    1
            );

            assertEquals("vehicle-1", dto.vehicleId());
            assertEquals("mission-1", dto.missionId());
            assertEquals("arm", dto.commandCode());
            assertEquals(Map.of("altitude", 100.0), dto.arguments());
            assertEquals(1, dto.priority());
        }

        @Test
        @DisplayName("Should throw exception when vehicleId is null")
        void shouldThrowExceptionWhenVehicleIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CommandExecutionDTO(null, "mission-1", "arm", null, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when vehicleId is blank")
        void shouldThrowExceptionWhenVehicleIdIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CommandExecutionDTO("  ", "mission-1", "arm", null, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when commandCode is null")
        void shouldThrowExceptionWhenCommandCodeIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CommandExecutionDTO("vehicle-1", "mission-1", null, null, null)
            );
        }

        @Test
        @DisplayName("Should throw exception when commandCode is blank")
        void shouldThrowExceptionWhenCommandCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                    new CommandExecutionDTO("vehicle-1", "mission-1", "", null, null)
            );
        }

        @Test
        @DisplayName("Should set default empty map when arguments is null")
        void shouldSetDefaultEmptyMapWhenArgumentsIsNull() {
            var dto = new CommandExecutionDTO("vehicle-1", "mission-1", "arm", null, null);

            assertNotNull(dto.arguments());
            assertTrue(dto.arguments().isEmpty());
        }

        @Test
        @DisplayName("Should set default priority to 0 when null")
        void shouldSetDefaultPriorityWhenNull() {
            var dto = new CommandExecutionDTO("vehicle-1", "mission-1", "arm", null, null);

            assertEquals(0, dto.priority());
        }
    }
}
