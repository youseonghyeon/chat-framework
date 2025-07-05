package io.github.youseonghyeon.broadcast.impl;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;
import io.github.youseonghyeon.dto.Message;

import java.util.function.BiConsumer;

/**
 * 단일 노드 환경에서 메시지를 브로드캐스트하지 않는 구현체입니다.
 */
public class NoOpBroadcaster implements MessageBroadcaster<Message> {

    @Override
    public void broadcast(Long roomId, Message message) {
        // No operation: 메시지를 브로드캐스트하지 않습니다.
        // 이 구현체는 단일 노드 환경에서 사용되며, 메시지를 전송할 필요가 없습니다.
    }

    @Override
    public void onMessage(BiConsumer<Long, Message> callback) {

    }
}
