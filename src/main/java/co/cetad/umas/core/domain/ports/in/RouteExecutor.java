package co.cetad.umas.core.domain.ports.in;

import co.cetad.umas.core.domain.model.dto.RouteExecutionDTO;
import co.cetad.umas.core.domain.model.dto.RouteExecutionResult;

import java.util.concurrent.CompletableFuture;

/**
 * Inbound port for executing routes with sequential command execution.
 * Commands are executed one at a time, waiting for confirmation before proceeding.
 */
@FunctionalInterface
public interface RouteExecutor {

    /**
     * Executes a route with sequential command execution.
     * Each command waits for confirmation before the next one starts.
     *
     * @param route the route to execute
     * @return CompletableFuture with the execution result
     */
    CompletableFuture<RouteExecutionResult> executeRoute(RouteExecutionDTO route);

}