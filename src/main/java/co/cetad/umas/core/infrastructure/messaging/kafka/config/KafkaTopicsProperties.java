package co.cetad.umas.core.infrastructure.messaging.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {

    private String telemetry = "ugcs.drone.telemetry";

    private String events = "ugcs.drone.events";

    private String commands = "ugcs.drone.execute";

    private String vehicleStatus = "ugcs.drone.vehicle.status";

    private String routeStatus = "ugcs.drone.route.status";

}