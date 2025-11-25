package co.cetad.umas.core.application.service.route;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import co.cetad.umas.core.domain.model.dto.RouteExecutionDTO;
import co.cetad.umas.core.domain.model.dto.RouteExecutionResult;
import co.cetad.umas.core.domain.model.dto.WaypointDTO;
import co.cetad.umas.core.domain.ports.in.RouteExecutor;
import co.cetad.umas.core.domain.ports.out.VehicleStateMonitor;
import co.cetad.umas.core.infrastructure.config.RouteExecutionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio que ejecuta una secuencia FIJA de comandos:
 * 1. ARM
 * 2. TAKEOFF
 * 3. Para cada waypoint: navegar y esperar
 * 4. LAND
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteExecutionService implements RouteExecutor {



}