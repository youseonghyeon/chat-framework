package io.github.youseonghyeon.broadcast.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.*;

abstract class KafkaLifecycleManager {

    // TODO OOM 발생 문제를 제거하기 위해 queue size 제한 필요
    private ExecutorService executorService;
    private KafkaConsumer<?, ?> consumerRef;
    private KafkaProducer<?, ?> producerRef;
    private final Runnable closeResource = () -> {
        if (producerRef != null) {
            producerRef.close();
        }
        if (consumerRef != null) {
            consumerRef.wakeup();
            consumerRef.close();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            if (!executorService.isTerminated()) {
                try {
                    boolean result = executorService.awaitTermination(5, TimeUnit.SECONDS);
                    if (!result) {
                        System.err.println("Executor service did not terminate in the specified time.");
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    };


    protected KafkaLifecycleManager() {
        this.executorService = initThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(closeResource, "KafkaLifecycleManager-ShutdownHook"));
    }

    private ExecutorService initThreadPool() {
        return new ThreadPoolExecutor(10,
                50,
                5, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1000),
                kafkaThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private static @NotNull ThreadFactory kafkaThreadFactory() {
        return runner -> {
            Thread thread = new Thread(runner);
            thread.setName("kafka-runner-" + thread.getId());
            return thread;
        };
    }

    protected Future<?> submit(Runnable task) {
        Objects.requireNonNull(task);
        return executorService.submit(task);
    }

    protected Future<?> submit(Callable<?> task) {
        Objects.requireNonNull(task);
        return executorService.submit(task);
    }


    protected <K, V> KafkaConsumer<K, V> createConsumer(Properties properties) {
        KafkaConsumer<K, V> kvKafkaConsumer = new KafkaConsumer<>(properties);
        consumerRef = kvKafkaConsumer;
        return kvKafkaConsumer;
    }

    protected <K, V> KafkaProducer<K, V> createProducer(Properties properties) {
        KafkaProducer<K, V> kvKafkaProducer = new KafkaProducer<>(properties);
        producerRef = kvKafkaProducer;
        return kvKafkaProducer;
    }

}
