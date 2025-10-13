package co.cetad.umas.core.domain.ports.in;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface EventProcessor<I, O> {

    CompletableFuture<O> process(I event);

}