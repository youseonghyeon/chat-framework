package io.github.youseonghyeon.broadcast.impl;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;

import java.util.function.BiConsumer;

/**
 * 단일 노드 환경에서 메시지를 브로드캐스트하지 않는 구현체입니다.
 */
public class NoOpBroadcaster implements MessageBroadcaster<Void> {

    @Override
    public void broadcast(Long roomId, Void message) {
        // No operation: 메시지를 브로드캐스트하지 않습니다.
        // 단일 노드 환경에서는 메시지를 전송할 필요가 없습니다.
    }

    @Override
    public void onMessage(BiConsumer<Long, Void> callback) {

    }
}
