package io.github.youseonghyeon.broadcast.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

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


    protected KafkaLifecycleManager(Integer callbackRunnerThreadCount) {
        this.executorService = initThreadPool(callbackRunnerThreadCount);
        Runtime.getRuntime().addShutdownHook(new Thread(closeResource, "KafkaLifecycleManager-ShutdownHook"));
    }

    private ExecutorService initThreadPool(Integer callbackRunnerThreadCount) {
        return Executors.newFixedThreadPool(callbackRunnerThreadCount, r -> {
            Thread t = new Thread(r);
            t.setName("kafka-callback-runner-" + t.getId());
            return t;
        });
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
