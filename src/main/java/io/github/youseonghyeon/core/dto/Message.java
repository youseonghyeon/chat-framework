package io.github.youseonghyeon.core.dto;

import io.github.youseonghyeon.core.event.EventType;

import java.io.Serializable;
import java.nio.channels.SocketChannel;

public record Message(EventType eventType, String roomId, byte[] header, byte[] content, SocketChannel socketChannel) implements Serializable {

}
