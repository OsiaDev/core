package co.cetad.umas.core.infrastructure.ugcs.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UgcsProperties Tests")
class UgcsPropertiesTest {

    private UgcsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new UgcsProperties();
    }

    @Nested
    @DisplayName("Server properties tests")
    class ServerPropertiesTests {

        @Test
        @DisplayName("Should have default server host")
        void shouldHaveDefaultServerHost() {
            assertEquals("localhost", properties.getServer().getHost());
        }

        @Test
        @DisplayName("Should have default server port")
        void shouldHaveDefaultServerPort() {
            assertEquals(3334, properties.getServer().getPort());
        }

        @Test
        @DisplayName("Should set server host")
        void shouldSetServerHost() {
            properties.getServer().setHost("192.168.1.100");
            assertEquals("192.168.1.100", properties.getServer().getHost());
        }

        @Test
        @DisplayName("Should set server port")
        void shouldSetServerPort() {
            properties.getServer().setPort(5000);
            assertEquals(5000, properties.getServer().getPort());
        }
    }

    @Nested
    @DisplayName("Credentials properties tests")
    class CredentialsPropertiesTests {

        @Test
        @DisplayName("Should have default login")
        void shouldHaveDefaultLogin() {
            assertEquals("admin", properties.getCredentials().getLogin());
        }

        @Test
        @DisplayName("Should have default password")
        void shouldHaveDefaultPassword() {
            assertEquals("admin", properties.getCredentials().getPassword());
        }

        @Test
        @DisplayName("Should set login")
        void shouldSetLogin() {
            properties.getCredentials().setLogin("custom_user");
            assertEquals("custom_user", properties.getCredentials().getLogin());
        }

        @Test
        @DisplayName("Should set password")
        void shouldSetPassword() {
            properties.getCredentials().setPassword("custom_password");
            assertEquals("custom_password", properties.getCredentials().getPassword());
        }
    }

    @Nested
    @DisplayName("Reconnect properties tests")
    class ReconnectPropertiesTests {

        @Test
        @DisplayName("Should have reconnect enabled by default")
        void shouldHaveReconnectEnabledByDefault() {
            assertTrue(properties.getReconnect().isEnabled());
        }

        @Test
        @DisplayName("Should have default initial delay")
        void shouldHaveDefaultInitialDelay() {
            assertEquals(5000, properties.getReconnect().getInitialDelay());
        }

        @Test
        @DisplayName("Should have default max delay")
        void shouldHaveDefaultMaxDelay() {
            assertEquals(60000, properties.getReconnect().getMaxDelay());
        }

        @Test
        @DisplayName("Should have default multiplier")
        void shouldHaveDefaultMultiplier() {
            assertEquals(2.0, properties.getReconnect().getMultiplier());
        }

        @Test
        @DisplayName("Should set reconnect enabled")
        void shouldSetReconnectEnabled() {
            properties.getReconnect().setEnabled(false);
            assertFalse(properties.getReconnect().isEnabled());
        }

        @Test
        @DisplayName("Should set initial delay")
        void shouldSetInitialDelay() {
            properties.getReconnect().setInitialDelay(1000);
            assertEquals(1000, properties.getReconnect().getInitialDelay());
        }

        @Test
        @DisplayName("Should set max delay")
        void shouldSetMaxDelay() {
            properties.getReconnect().setMaxDelay(120000);
            assertEquals(120000, properties.getReconnect().getMaxDelay());
        }

        @Test
        @DisplayName("Should set multiplier")
        void shouldSetMultiplier() {
            properties.getReconnect().setMultiplier(1.5);
            assertEquals(1.5, properties.getReconnect().getMultiplier());
        }
    }

    @Nested
    @DisplayName("Full properties tests")
    class FullPropertiesTests {

        @Test
        @DisplayName("Should allow chained property access")
        void shouldAllowChainedPropertyAccess() {
            properties.getServer().setHost("custom-host");
            properties.getServer().setPort(4444);
            properties.getCredentials().setLogin("user");
            properties.getCredentials().setPassword("pass");
            properties.getReconnect().setEnabled(false);

            assertEquals("custom-host", properties.getServer().getHost());
            assertEquals(4444, properties.getServer().getPort());
            assertEquals("user", properties.getCredentials().getLogin());
            assertEquals("pass", properties.getCredentials().getPassword());
            assertFalse(properties.getReconnect().isEnabled());
        }

        @Test
        @DisplayName("Should set new Server object")
        void shouldSetNewServerObject() {
            var server = new UgcsProperties.Server();
            server.setHost("new-host");
            server.setPort(9999);
            properties.setServer(server);

            assertEquals("new-host", properties.getServer().getHost());
            assertEquals(9999, properties.getServer().getPort());
        }

        @Test
        @DisplayName("Should set new Credentials object")
        void shouldSetNewCredentialsObject() {
            var credentials = new UgcsProperties.Credentials();
            credentials.setLogin("new-user");
            credentials.setPassword("new-pass");
            properties.setCredentials(credentials);

            assertEquals("new-user", properties.getCredentials().getLogin());
            assertEquals("new-pass", properties.getCredentials().getPassword());
        }

        @Test
        @DisplayName("Should set new Reconnect object")
        void shouldSetNewReconnectObject() {
            var reconnect = new UgcsProperties.Reconnect();
            reconnect.setEnabled(false);
            reconnect.setInitialDelay(2000);
            properties.setReconnect(reconnect);

            assertFalse(properties.getReconnect().isEnabled());
            assertEquals(2000, properties.getReconnect().getInitialDelay());
        }
    }
}
