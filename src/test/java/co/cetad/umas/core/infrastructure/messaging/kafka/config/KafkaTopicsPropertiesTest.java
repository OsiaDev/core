package co.cetad.umas.core.infrastructure.messaging.kafka.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KafkaTopicsProperties Tests")
class KafkaTopicsPropertiesTest {

    private KafkaTopicsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KafkaTopicsProperties();
    }

    @Nested
    @DisplayName("Default values tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should have default telemetry topic")
        void shouldHaveDefaultTelemetryTopic() {
            assertEquals("umas.drone.telemetry", properties.getTelemetry());
        }

        @Test
        @DisplayName("Should have default events topic")
        void shouldHaveDefaultEventsTopic() {
            assertEquals("umas.drone.events", properties.getEvents());
        }

        @Test
        @DisplayName("Should have default commands topic")
        void shouldHaveDefaultCommandsTopic() {
            assertEquals("umas.drone.execute", properties.getCommands());
        }

        @Test
        @DisplayName("Should have default routes topic")
        void shouldHaveDefaultRoutesTopic() {
            assertEquals("umas.drone.route.execute", properties.getRoutes());
        }

        @Test
        @DisplayName("Should have default vehicle status topic")
        void shouldHaveDefaultVehicleStatusTopic() {
            assertEquals("umas.drone.vehicle.status", properties.getVehicleStatus());
        }

        @Test
        @DisplayName("Should have default route status topic")
        void shouldHaveDefaultRouteStatusTopic() {
            assertEquals("umas.drone.route.status", properties.getRouteStatus());
        }

        @Test
        @DisplayName("Should have default mission status topic")
        void shouldHaveDefaultMissionStatusTopic() {
            assertEquals("umas.drone.mission.status", properties.getMissionStatus());
        }
    }

    @Nested
    @DisplayName("Setter tests")
    class SetterTests {

        @Test
        @DisplayName("Should set telemetry topic")
        void shouldSetTelemetryTopic() {
            properties.setTelemetry("custom.telemetry");
            assertEquals("custom.telemetry", properties.getTelemetry());
        }

        @Test
        @DisplayName("Should set events topic")
        void shouldSetEventsTopic() {
            properties.setEvents("custom.events");
            assertEquals("custom.events", properties.getEvents());
        }

        @Test
        @DisplayName("Should set commands topic")
        void shouldSetCommandsTopic() {
            properties.setCommands("custom.commands");
            assertEquals("custom.commands", properties.getCommands());
        }

        @Test
        @DisplayName("Should set routes topic")
        void shouldSetRoutesTopic() {
            properties.setRoutes("custom.routes");
            assertEquals("custom.routes", properties.getRoutes());
        }

        @Test
        @DisplayName("Should set vehicle status topic")
        void shouldSetVehicleStatusTopic() {
            properties.setVehicleStatus("custom.vehicle.status");
            assertEquals("custom.vehicle.status", properties.getVehicleStatus());
        }

        @Test
        @DisplayName("Should set route status topic")
        void shouldSetRouteStatusTopic() {
            properties.setRouteStatus("custom.route.status");
            assertEquals("custom.route.status", properties.getRouteStatus());
        }

        @Test
        @DisplayName("Should set mission status topic")
        void shouldSetMissionStatusTopic() {
            properties.setMissionStatus("custom.mission.status");
            assertEquals("custom.mission.status", properties.getMissionStatus());
        }
    }
}
