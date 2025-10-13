package co.cetad.umas.core.domain.ports.in;

import co.cetad.umas.core.domain.model.dto.CommandExecutionDTO;
import co.cetad.umas.core.domain.model.dto.CommandResultDTO;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface CommandExecutor {

    Mono<CommandResultDTO> execute(CommandExecutionDTO command);

}