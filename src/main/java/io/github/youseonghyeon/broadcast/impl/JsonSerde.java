package io.github.youseonghyeon.broadcast.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/// 테스트용
public class JsonSerde<T> implements Serde<T> {

    private final Logger log = LoggerFactory.getLogger(JsonSerde.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Serializer<T> serializer;
    private final Deserializer<T> deserializer;

    public JsonSerde(Class<T> type) {
        this.serializer = new DefaultJsonSerializer<>();
        this.deserializer = new DefaultJsonDeserializer<>(type);
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }

    private class DefaultJsonSerializer<E> implements Serializer<E> {
        @Override
        public byte[] serialize(String topic, E data) {
            try {
                return objectMapper.writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                log.error("Error serializing message: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    private class DefaultJsonDeserializer<T> implements Deserializer<T> {

        private final Class<T> type;

        public DefaultJsonDeserializer(Class<T> type) {
            this.type = type;
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            try {
                return objectMapper.readValue(data, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize", e);
            }
        }
    }
}
