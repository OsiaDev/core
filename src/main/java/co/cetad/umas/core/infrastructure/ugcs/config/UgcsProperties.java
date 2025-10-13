package co.cetad.umas.core.infrastructure.ugcs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ugcs")
public class UgcsProperties {

    private Server server = new Server();
    private Credentials credentials = new Credentials();
    private Reconnect reconnect = new Reconnect();

    @Data
    public static class Server {
        private String host = "localhost";
        private int port = 3334;
    }

    @Data
    public static class Credentials {
        private String login = "admin";
        private String password = "admin";
    }

    @Data
    public static class Reconnect {
        private boolean enabled = true;
        private long initialDelay = 5000;
        private long maxDelay = 60000;
        private double multiplier = 2.0;
    }

}
