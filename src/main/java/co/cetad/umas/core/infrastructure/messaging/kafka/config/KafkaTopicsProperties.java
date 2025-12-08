package co.cetad.umas.core.infrastructure.messaging.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsProperties {

    private String telemetry = "umas.drone.telemetry";

    private String events = "umas.drone.events";

    private String commands = "umas.drone.execute";

    private String routes = "umas.drone.route.execute";

    private String vehicleStatus = "umas.drone.vehicle.status";

    private String routeStatus = "umas.drone.route.status";

    private String missionStatus = "umas.drone.mission.status";

}