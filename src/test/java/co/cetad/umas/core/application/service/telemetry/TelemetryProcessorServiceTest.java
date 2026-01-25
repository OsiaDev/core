package co.cetad.umas.core.application.service.telemetry;

import co.cetad.umas.core.domain.model.vo.DroneLocation;
import co.cetad.umas.core.domain.model.vo.TelemetryData;
import co.cetad.umas.core.domain.ports.out.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelemetryProcessorService Tests")
class TelemetryProcessorServiceTest {

    @Mock
    private EventPublisher<TelemetryData> telemetryPublisher;

    private TelemetryProcessorService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryProcessorService(telemetryPublisher);
    }

    @Nested
    @DisplayName("Successful processing tests")
    class SuccessfulProcessingTests {

        @Test
        @DisplayName("Should process telemetry and publish successfully")
        void shouldProcessTelemetryAndPublishSuccessfully() throws Exception {
            var telemetry = createTelemetry("vehicle-1", 45.0, -73.0, 100.0);

            when(telemetryPublisher.publish(any())).thenReturn(Mono.empty());

            var result = service.process(telemetry).get();

            assertNull(result);
            verify(telemetryPublisher).publish(telemetry);
        }

        @Test
        @DisplayName("Should publish telemetry with all fields")
        void shouldPublishTelemetryWithAllFields() throws Exception {
            var location = DroneLocation.of(45.0, -73.0, 100.0);
            var fields = Map.<String, Object>of(
                    "groundSpeed", 15.5,
                    "batteryLevel", 85.0,
                    "heading", 270.0
            );
            var telemetry = new TelemetryData("vehicle-1", location, fields, LocalDateTime.now());

            when(telemetryPublisher.publish(any())).thenReturn(Mono.empty());

            service.process(telemetry).get();

            verify(telemetryPublisher).publish(argThat(data ->
                    data.vehicleId().equals("vehicle-1") &&
                            data.getSpeed().isPresent() &&
                            data.getBatteryLevel().isPresent()
            ));
        }
    }

    private TelemetryData createTelemetry(String vehicleId, double lat, double lon, double alt) {
        var location = DroneLocation.of(lat, lon, alt);
        return new TelemetryData(vehicleId, location, Map.of(), LocalDateTime.now());
    }
}
