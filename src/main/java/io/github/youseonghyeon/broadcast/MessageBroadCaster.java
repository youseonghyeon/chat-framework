package io.github.youseonghyeon.broadcast;

import io.github.youseonghyeon.core.dto.Message;

public interface MessageBroadCaster {

    void broadcast(Object identifier, Message message);

}
