package co.cetad.umas.core.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuración de Executors personalizados para procesamiento asíncrono
 *
 * EXECUTORS DISPONIBLES:
 * 1. droneExecutor: Para procesamiento de drones (I/O bound)
 * 2. virtualThreadExecutor: Para Java 21+ (I/O bound intensivo)
 * 3. cpuBoundExecutor: Para operaciones CPU-intensive
 *
 * CONFIGURACIÓN:
 * - Properties definidas en application.yml
 * - Beans con @Qualifier para inyección específica
 * - Thread factories con nombres descriptivos
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Value("${executor.drone.core-pool-size:10}")
    private int dronePoolCoreSize;

    @Value("${executor.drone.max-pool-size:50}")
    private int dronePoolMaxSize;

    @Value("${executor.drone.queue-capacity:100}")
    private int droneQueueCapacity;

    @Value("${executor.drone.keep-alive-seconds:60}")
    private long droneKeepAliveSeconds;

    /**
     * Executor para procesamiento de drones (I/O bound)
     *
     * Características:
     * - Pool dimensionado para operaciones de red (UgCS, Redis)
     * - Queue capacity para manejar picos de carga
     * - CallerRunsPolicy: si está lleno, ejecuta en el thread que llama
     * - Threads con nombres descriptivos para debugging
     *
     * Usar con: @Qualifier("droneExecutor")
     */
    @Bean(name = "droneExecutor", destroyMethod = "shutdown")
    public ExecutorService droneExecutor() {  // ✅ Cambiar de Executor a ExecutorService
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                dronePoolCoreSize,
                dronePoolMaxSize,
                droneKeepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(droneQueueCapacity),
                new NamedThreadFactory("drone-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.allowCoreThreadTimeOut(true);

        log.info("🚁 Drone Executor initialized: core={}, max={}, queue={}, keepAlive={}s",
                dronePoolCoreSize, dronePoolMaxSize, droneQueueCapacity, droneKeepAliveSeconds);

        return executor;
    }

    /**
     * Virtual Thread Executor (Java 21+)
     *
     * Características:
     * - Threads virtuales ultra ligeros (1 MB vs 1 GB de platform threads)
     * - Ideal para operaciones I/O bound intensivas
     * - Escalabilidad a millones de threads
     * - Perfect para llamadas a APIs, polling, esperas
     *
     * Usar con: @Qualifier("virtualThreadExecutor")
     *
     * NOTA: Requiere Java 21 o superior
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        log.info("🌟 Virtual Thread Executor initialized (Java 21+)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Executor para operaciones CPU-intensive
     *
     * Características:
     * - Tamaño óptimo: número de procesadores disponibles
     * - Fixed pool (no crece ni decrece)
     * - Usar para: cálculos, transformaciones, procesamiento local
     *
     * Usar con: @Qualifier("cpuBoundExecutor")
     */
    @Bean(name = "cpuBoundExecutor", destroyMethod = "shutdown")
    public ExecutorService cpuBoundExecutor() {  // ✅ Cambiar de Executor a ExecutorService
        int processors = Runtime.getRuntime().availableProcessors();

        log.info("💻 CPU Bound Executor initialized with {} threads", processors);

        return Executors.newFixedThreadPool(
                processors,
                new NamedThreadFactory("cpu-executor")
        );
    }

    /**
     * ThreadFactory personalizado para crear threads con nombres descriptivos
     * Facilita el debugging y monitoreo
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);

            // Configurar handler para excepciones no capturadas
            thread.setUncaughtExceptionHandler((t, e) ->
                    log.error("Uncaught exception in thread {}", t.getName(), e)
            );

            return thread;
        }
    }

    /**
     * BONUS: Executor con métricas (opcional)
     * Para monitorear el estado del pool
     */
    @Bean(name = "monitoredDroneExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor monitoredDroneExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                dronePoolCoreSize,
                dronePoolMaxSize,
                droneKeepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(droneQueueCapacity),
                new NamedThreadFactory("monitored-drone-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.allowCoreThreadTimeOut(true);

        // Log de métricas periódicamente
        scheduleMetricsLogging(executor);

        log.info("📊 Monitored Drone Executor initialized with metrics");

        return executor;
    }

    private void scheduleMetricsLogging(ThreadPoolExecutor executor) {
        Executors.newSingleThreadScheduledExecutor(
                new NamedThreadFactory("executor-metrics")
        ).scheduleAtFixedRate(
                () -> logExecutorMetrics(executor),
                30, // initial delay
                30, // period
                TimeUnit.SECONDS
        );
    }

    private void logExecutorMetrics(ThreadPoolExecutor executor) {
        log.debug("Executor Metrics - Active: {}, Pool Size: {}, Queue: {}, Completed: {}",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

}