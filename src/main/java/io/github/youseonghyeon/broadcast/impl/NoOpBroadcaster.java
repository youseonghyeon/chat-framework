package io.github.youseonghyeon.broadcast.impl;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;

/**
 * 단일 노드 환경에서 메시지를 브로드캐스트하지 않는 구현체입니다.
 */
public class NoOpBroadcaster implements MessageBroadcaster {

    @Override
    public void broadcast(Object obj) {
        // No operation performed
    }

}
