package co.cetad.umas.core.domain.ports.out;

import co.cetad.umas.core.domain.model.dto.VehicleStatusDTO;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface StatusNotifier {

    Mono<Void> notify(VehicleStatusDTO status);

}