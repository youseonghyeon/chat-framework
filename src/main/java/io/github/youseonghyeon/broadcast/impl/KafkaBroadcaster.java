package io.github.youseonghyeon.broadcast.impl;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;

import java.util.UUID;

public abstract class KafkaBroadcaster implements MessageBroadcaster {

    // 브로드케스팅 중복을 제어하기 위한 노드 ID
    private static final String nodeId = UUID.randomUUID().toString();

}
