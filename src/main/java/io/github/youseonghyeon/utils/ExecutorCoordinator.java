package io.github.youseonghyeon.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ExecutorCoordinator {

    private static final Logger log = LoggerFactory.getLogger(ExecutorCoordinator.class);

    public static void shutdownSequential(ExecutorService... executorServices) {
        Stream.of(executorServices).forEach(es -> {
            if (es == null) {
                throw new IllegalArgumentException("ExecutorService cannot be null");
            }

            if (!es.isShutdown()) {
                es.shutdown();
                try {
                    boolean shutdownResult = es.awaitTermination(2, TimeUnit.SECONDS);
                    if (shutdownResult) {
                        log.info("Executor service {} shutdown gracefully", es);
                    } else {
                        log.warn("Executor service {} did not terminate in the specified time, forcing shutdown", es);
                        es.shutdownNow();
                        boolean killed = es.awaitTermination(10, TimeUnit.SECONDS);
                        if (!killed) log.error("Executor service {} did not terminate after forced shutdown", es);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Executor service shutdown interrupted", e);
                }
            }
        });
    }
}
