package io.github.youseonghyeon.broadcast.no;

import io.github.youseonghyeon.broadcast.MessageBroadCaster;
import io.github.youseonghyeon.core.dto.Message;

/**
 * 단일 노드 환경에서 메시지를 브로드캐스트하지 않는 구현체입니다.
 */
public class NoOpsBroadcaster implements MessageBroadCaster {

    @Override
    public void broadcast(Object identifier, Message message) {
    }
}
