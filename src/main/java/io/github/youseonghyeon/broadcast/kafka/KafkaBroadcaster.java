package io.github.youseonghyeon.broadcast.kafka;

import io.github.youseonghyeon.broadcast.MessageBroadCaster;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 초기화 가능 영역과 지연 처리 필요 영역을 분리해야 함
 * 분리 후 lazy initialization interface 구현하는 것으로 함
 * 해당 구현 후 Engine쪽에서 instaceof를 통해 lazy initialization interface를 구현한 클래스를 처리 해야함
 */
public class KafkaBroadcaster extends KafkaLifecycleManager implements MessageBroadCaster, LazyInitializer {

    private final Logger log = LoggerFactory.getLogger(KafkaBroadcaster.class);

    private final String DEFAULT_CONSUMER_GROUP_ID_PREFIX = "chat-group";
    private final String DEFAULT_TOPIC = "chat-topic";

    private KafkaConsumer<String, Message> consumer;
    private KafkaProducer<String, Message> producer;
    private final AtomicBoolean consumerRunning = new AtomicBoolean(true);
    private ChatEventPublisher chatEventPublisher;
    private ThreadPoolExecutor consumerThreadExecutor;

    // 브로드케스팅 중복을 제어하기 위한 노드 ID
    private static final String nodeId = UUID.randomUUID().toString();

    public KafkaBroadcaster(@Nullable String consumerGroupIdPrefix, Properties properties, ChatEventPublisher chatEventPublisher) {
        super();
        if (properties == null) {
            throw new IllegalArgumentException("Properties must not be null.");
        }
        if (chatEventPublisher == null) {
            throw new IllegalArgumentException("ChatEventPublisher must not be null.");
        }
        if (consumerGroupIdPrefix == null || consumerGroupIdPrefix.isBlank()) {
            consumerGroupIdPrefix = DEFAULT_CONSUMER_GROUP_ID_PREFIX;
        }
        validateDefaultProperties(properties);
        initProducer(properties);
        initConsumer(properties, consumerGroupIdPrefix + "-" + nodeId, DEFAULT_TOPIC);
    }

    @Override
    public void initialize() {
        // 지연 처리 필요한것들을 여기서 실행, consumer와 producer 초기화 등이 있음
    }


    @Override
    public void broadcast(@Nullable Object identifier, Message message) {
        // publish start
        ProducerRecord<String, Message> record = new ProducerRecord<>(DEFAULT_TOPIC, message.roomId(), message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Error sending message: {}", exception.getMessage());
            } else {
                log.debug("Message sent successfully: topic = {}, partition = {}, offset = {}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });

    }

    private void initConsumer(Properties properties, String groupId, String topicName) {
        Properties props = new Properties(properties);
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumer = createConsumer(props);
        consumer.subscribe(Collections.singleton(topicName));

        while (consumerRunning.get()) {
            consumer.poll(Duration.ofMillis(100))
                    .forEach(record -> chatEventPublisher.publish(record.value()));
        }
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

}
