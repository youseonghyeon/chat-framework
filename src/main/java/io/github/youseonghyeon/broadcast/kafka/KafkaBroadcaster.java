package io.github.youseonghyeon.broadcast.kafka;

import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class KafkaBroadcaster<E extends Message> extends KafkaLifecycleManager implements MessageSubscriber {

    private final Logger log = LoggerFactory.getLogger(KafkaBroadcaster.class);

    private final String DEFAULT_CONSUMER_GROUP_ID_PREFIX = "chat-group";
    private final String DEFAULT_TOPIC = "chat-topic";
    private static final int DEFAULT_CALLBACK_RUNNER_THREAD_COUNT = 5;

    private KafkaConsumer<String, E> consumer;
    private KafkaProducer<String, E> producer;
    private final AtomicBoolean consumerRunning = new AtomicBoolean(false);
    private ChatEventPublisher chatEventPublisher;


    // 브로드케스팅 중복을 제어하기 위한 노드 ID
    private static final String nodeId = UUID.randomUUID().toString();

    public KafkaBroadcaster(String consumerGroupIdPrefix, Properties properties, ChatEventPublisher chatEventPublisher) {
        super(DEFAULT_CALLBACK_RUNNER_THREAD_COUNT);
        if (consumerGroupIdPrefix == null || consumerGroupIdPrefix.isBlank()) {
            consumerGroupIdPrefix = DEFAULT_CONSUMER_GROUP_ID_PREFIX;
        }
        validateDefaultProperties(properties);
        initProducer(properties);
        initConsumer(properties, consumerGroupIdPrefix + "-" + nodeId, DEFAULT_TOPIC);

        this.chatEventPublisher = chatEventPublisher;
    }

    private void initConsumer(Properties properties, String groupId, String topicName) {
        Properties props = new Properties(properties);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumer = createConsumer(props);
        consumer.subscribe(Collections.singleton(topicName));
    }

    private void initProducer(Properties properties) {
        producer = createProducer(properties);
    }

    private void validateDefaultProperties(Properties properties) {
        if (properties == null || properties.isEmpty()) {
            throw new IllegalArgumentException("Properties must not be null or empty.");
        }
        if (!properties.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new IllegalArgumentException("Bootstrap servers must be specified.");
        }
    }

    public void broadcast(E message) {
        ProducerRecord<String, E> record = new ProducerRecord<>(DEFAULT_TOPIC, message.roomId(), message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending message: {}", exception.getMessage());
            } else {
                log.debug("Message sent successfully: topic = {}, partition = {}, offset = {}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }


    public void onMessage(BiConsumer<String, E> callback) {
        boolean result = consumerRunning.compareAndExchange(false, true);
        if (result) {
            log.warn("Consumer is already running. Skipping onMessage registration.");
            return;
        }
        submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, E> records = consumer.poll(Duration.ofMillis(100));
                records.forEach(record -> submit(() -> callback.accept(record.key(), record.value())));
            }
        });
    }

    @Override
    public void subscribe(Message Message) {

    }

    @Override
    public void init() {
        // TODO 메시지 consumer 쓰레드 생성

//        chatEventPublisher.publish(message);

    }
}
