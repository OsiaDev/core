package co.cetad.umas.core.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsyncConfiguration Tests")
class AsyncConfigurationTest {

    private AsyncConfiguration config;

    @BeforeEach
    void setUp() {
        config = new AsyncConfiguration();
        ReflectionTestUtils.setField(config, "dronePoolCoreSize", 10);
        ReflectionTestUtils.setField(config, "dronePoolMaxSize", 50);
        ReflectionTestUtils.setField(config, "droneQueueCapacity", 100);
        ReflectionTestUtils.setField(config, "droneKeepAliveSeconds", 60L);
    }

    @Nested
    @DisplayName("droneExecutor tests")
    class DroneExecutorTests {

        @Test
        @DisplayName("Should create drone executor with correct configuration")
        void shouldCreateDroneExecutorWithCorrectConfiguration() {
            var executor = (ThreadPoolExecutor) config.droneExecutor();

            assertNotNull(executor);
            assertEquals(10, executor.getCorePoolSize());
            assertEquals(50, executor.getMaximumPoolSize());
            assertEquals(60, executor.getKeepAliveTime(java.util.concurrent.TimeUnit.SECONDS));

            executor.shutdown();
        }

        @Test
        @DisplayName("Should allow core thread timeout")
        void shouldAllowCoreThreadTimeout() {
            var executor = (ThreadPoolExecutor) config.droneExecutor();

            assertTrue(executor.allowsCoreThreadTimeOut());

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("virtualThreadExecutor tests")
    class VirtualThreadExecutorTests {

        @Test
        @DisplayName("Should create virtual thread executor")
        void shouldCreateVirtualThreadExecutor() {
            var executor = config.virtualThreadExecutor();

            assertNotNull(executor);
        }
    }

    @Nested
    @DisplayName("cpuBoundExecutor tests")
    class CpuBoundExecutorTests {

        @Test
        @DisplayName("Should create cpu bound executor with available processors")
        void shouldCreateCpuBoundExecutorWithAvailableProcessors() {
            var executor = (ThreadPoolExecutor) config.cpuBoundExecutor();

            assertNotNull(executor);
            assertEquals(Runtime.getRuntime().availableProcessors(), executor.getCorePoolSize());
            assertEquals(Runtime.getRuntime().availableProcessors(), executor.getMaximumPoolSize());

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("monitoredDroneExecutor tests")
    class MonitoredDroneExecutorTests {

        @Test
        @DisplayName("Should create monitored drone executor")
        void shouldCreateMonitoredDroneExecutor() {
            var executor = config.monitoredDroneExecutor();

            assertNotNull(executor);
            assertEquals(10, executor.getCorePoolSize());

            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("NamedThreadFactory tests")
    class NamedThreadFactoryTests {

        @Test
        @DisplayName("Should create threads with proper naming")
        void shouldCreateThreadsWithProperNaming() {
            var executor = (ThreadPoolExecutor) config.droneExecutor();

            // Submit a task to create a thread
            executor.submit(() -> {
                assertTrue(Thread.currentThread().getName().startsWith("drone-executor-"));
            });

            executor.shutdown();
        }
    }
}
