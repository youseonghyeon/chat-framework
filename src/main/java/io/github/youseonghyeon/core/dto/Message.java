package io.github.youseonghyeon.core.dto;

import io.github.youseonghyeon.core.event.EventType;

public record Message(EventType eventType, byte[] header, byte[] content, String roomId) {

}
