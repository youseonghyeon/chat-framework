package io.github.youseonghyeon.core.event;

import io.github.youseonghyeon.core.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final Map<EventType, MessageSubscriber> eventHandlerMap = new ConcurrentHashMap<>();

    public void registerSubscriber(EventType eventType, MessageSubscriber handler) {
        if (eventType == null || handler == null) {
            throw new IllegalArgumentException("Message type and handler cannot be null");
        }
        if (eventHandlerMap.containsKey(eventType)) {
            log.info("Event handler switched for message type: {}", eventType);
        }
        eventHandlerMap.put(eventType, handler);
    }

    public void publish(Message message) {
        log.info("Publishing message: {}", message);
        EventType subType = message.eventType();
        if (subType == null) throw new IllegalArgumentException("Message type cannot be null");
        MessageSubscriber messageSubscriber = eventHandlerMap.get(subType);
        if (messageSubscriber == null)
            throw new IllegalStateException("No handler registered for message type: " + subType);

        messageSubscriber.subscribe(message);
    }
}
