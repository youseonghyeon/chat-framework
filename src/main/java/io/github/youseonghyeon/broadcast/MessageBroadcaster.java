package io.github.youseonghyeon.broadcast;

import io.github.youseonghyeon.dto.Message;
import java.util.function.BiConsumer;

public interface MessageBroadcaster<E extends Message> {

    void broadcast(Long roomId, E message);

    void onMessage(BiConsumer<Long, E> callback);

}
