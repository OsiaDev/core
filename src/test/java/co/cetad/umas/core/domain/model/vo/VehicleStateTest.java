package co.cetad.umas.core.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VehicleState Tests")
class VehicleStateTest {

    @Nested
    @DisplayName("isOperational tests")
    class IsOperationalTests {

        @Test
        @DisplayName("ARMED should be operational")
        void armedShouldBeOperational() {
            assertTrue(VehicleState.ARMED.isOperational());
        }

        @Test
        @DisplayName("FLYING should be operational")
        void flyingShouldBeOperational() {
            assertTrue(VehicleState.FLYING.isOperational());
        }

        @Test
        @DisplayName("IDLE should not be operational")
        void idleShouldNotBeOperational() {
            assertFalse(VehicleState.IDLE.isOperational());
        }

        @Test
        @DisplayName("LANDING should not be operational")
        void landingShouldNotBeOperational() {
            assertFalse(VehicleState.LANDING.isOperational());
        }

        @Test
        @DisplayName("ERROR should not be operational")
        void errorShouldNotBeOperational() {
            assertFalse(VehicleState.ERROR.isOperational());
        }
    }

    @Nested
    @DisplayName("canExecuteCommand tests")
    class CanExecuteCommandTests {

        @ParameterizedTest
        @EnumSource(value = VehicleState.class, names = {"IDLE", "ARMED", "FLYING", "LANDING"})
        @DisplayName("Non-error states should be able to execute commands")
        void nonErrorStatesShouldBeAbleToExecuteCommands(VehicleState state) {
            assertTrue(state.canExecuteCommand());
        }

        @Test
        @DisplayName("ERROR state should not be able to execute commands")
        void errorStateShouldNotBeAbleToExecuteCommands() {
            assertFalse(VehicleState.ERROR.canExecuteCommand());
        }
    }

    @Nested
    @DisplayName("Enum basic tests")
    class EnumBasicTests {

        @Test
        @DisplayName("Should have all expected values")
        void shouldHaveAllExpectedValues() {
            VehicleState[] values = VehicleState.values();

            assertEquals(5, values.length);
            assertNotNull(VehicleState.IDLE);
            assertNotNull(VehicleState.ARMED);
            assertNotNull(VehicleState.FLYING);
            assertNotNull(VehicleState.LANDING);
            assertNotNull(VehicleState.ERROR);
        }

        @Test
        @DisplayName("Should parse from string")
        void shouldParseFromString() {
            assertEquals(VehicleState.IDLE, VehicleState.valueOf("IDLE"));
            assertEquals(VehicleState.ARMED, VehicleState.valueOf("ARMED"));
            assertEquals(VehicleState.FLYING, VehicleState.valueOf("FLYING"));
            assertEquals(VehicleState.LANDING, VehicleState.valueOf("LANDING"));
            assertEquals(VehicleState.ERROR, VehicleState.valueOf("ERROR"));
        }
    }
}
