package io.github.youseonghyeon.model;

import io.github.youseonghyeon.dto.Message;
import io.github.youseonghyeon.dto.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private Map<MessageType, Function<Message, ?>> eventHandlers = new ConcurrentHashMap<>();

    public ChatEventPublisher() {
    }

    public void registerHandler(MessageType messageType, Function<Message, ?> handler) {
        if (messageType == null || handler == null) {
            throw new IllegalArgumentException("Message type and handler cannot be null");
        }
        if (eventHandlers.containsKey(messageType)) {
            log.info("Event handler switched for message type: {}", messageType);
        }
        eventHandlers.put(messageType, handler);
    }

    public void publish(Message message) {
        MessageType messageType = message.getType();
        try {
            Function<Message, ?> handler = eventHandlers.get(messageType);
            if (handler != null) {
                handler.apply(message);
            } else {
                log.warn("No handler found for message type: {}", messageType);
            }
        } catch (Exception e) {
            log.error("Error processing message of type {}: {}", messageType, e.getMessage(), e);
        }


    }

}
