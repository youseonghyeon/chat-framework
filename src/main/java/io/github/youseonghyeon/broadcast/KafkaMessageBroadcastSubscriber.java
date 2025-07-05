package io.github.youseonghyeon.broadcast;

import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.MessageSubscriber;

public interface KafkaMessageBroadcastSubscriber<T extends Message> extends MessageSubscriber {

}
