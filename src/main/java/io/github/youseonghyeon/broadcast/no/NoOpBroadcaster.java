package io.github.youseonghyeon.broadcast.no;

import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;

/**
 * 단일 노드 환경에서 메시지를 브로드캐스트하지 않는 구현체입니다.
 */
public class NoOpBroadcaster implements MessageSubscriber {

    private static final EventType type = EventType.BROADCAST;

    @Override
    public void subscribe(Message Message) {

    }

    @Override
    public void init() {

    }
}
