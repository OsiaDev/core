package co.cetad.umas.core.domain.model.dto;

import co.cetad.umas.core.domain.model.vo.VehicleState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VehicleStatusDTO Tests")
class VehicleStatusDTOTest {

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create connected status")
        void shouldCreateConnectedStatus() {
            var status = VehicleStatusDTO.connected("vehicle-1");

            assertEquals("vehicle-1", status.vehicleId());
            assertEquals(VehicleState.IDLE, status.state());
            assertTrue(status.connected());
            assertNotNull(status.lastUpdate());
            assertNull(status.errorMessage());
        }

        @Test
        @DisplayName("Should create error status")
        void shouldCreateErrorStatus() {
            var status = VehicleStatusDTO.error("vehicle-1", "Connection timeout");

            assertEquals("vehicle-1", status.vehicleId());
            assertEquals(VehicleState.ERROR, status.state());
            assertFalse(status.connected());
            assertNotNull(status.lastUpdate());
            assertEquals("Connection timeout", status.errorMessage());
        }
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create status with all parameters")
        void shouldCreateStatusWithAllParameters() {
            var now = Instant.now();

            var status = new VehicleStatusDTO(
                    "vehicle-1",
                    VehicleState.FLYING,
                    true,
                    now,
                    null
            );

            assertEquals("vehicle-1", status.vehicleId());
            assertEquals(VehicleState.FLYING, status.state());
            assertTrue(status.connected());
            assertEquals(now, status.lastUpdate());
            assertNull(status.errorMessage());
        }

        @Test
        @DisplayName("Should create status with error message")
        void shouldCreateStatusWithErrorMessage() {
            var now = Instant.now();

            var status = new VehicleStatusDTO(
                    "vehicle-1",
                    VehicleState.ERROR,
                    false,
                    now,
                    "Low battery"
            );

            assertEquals("vehicle-1", status.vehicleId());
            assertEquals(VehicleState.ERROR, status.state());
            assertFalse(status.connected());
            assertEquals(now, status.lastUpdate());
            assertEquals("Low battery", status.errorMessage());
        }
    }
}
