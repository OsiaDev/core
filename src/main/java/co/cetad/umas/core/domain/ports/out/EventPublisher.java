package co.cetad.umas.core.domain.ports.out;

import reactor.core.publisher.Mono;

/**
 * Puerto de salida para publicar eventos
 */
@FunctionalInterface
public interface EventPublisher<T> {

    /**
     * Publica un evento en el topic correspondiente
     */
    Mono<Void> publish(T event);

}