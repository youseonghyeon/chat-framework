package io.github.youseonghyeon.core.event;

import io.github.youseonghyeon.core.dto.Message;

public interface MessageSubscriber {

    void subscribe(Message message);

    void init();
}
