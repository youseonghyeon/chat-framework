package io.github.youseonghyeon.broadcast;

import java.util.function.BiConsumer;

public interface MessageBroadcaster<E> {

    void broadcast(Long roomId, E message);

    void onMessage(BiConsumer<Long, E> callback);

}
