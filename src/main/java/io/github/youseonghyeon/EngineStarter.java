package io.github.youseonghyeon;

import io.github.youseonghyeon.broadcast.kafka.KafkaBroadcaster;
import io.github.youseonghyeon.core.ChatEngine;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;

public class EngineStarter {

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:19092");

        ChatEngine chatEngine = new ChatEngine();
        chatEngine.applyConfiguration(
                config -> config
                        .port(9999)
        );
        chatEngine.start();

        Runtime.getRuntime().addShutdownHook(new Thread(chatEngine::stop));
    }


}
