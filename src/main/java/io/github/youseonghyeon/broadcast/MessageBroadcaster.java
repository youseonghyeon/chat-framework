package io.github.youseonghyeon.broadcast;

import io.github.youseonghyeon.core.dto.Message;

public interface MessageBroadcaster<E extends Message> {

    void broadcast(Long roomId, E message);

    void init();

}
