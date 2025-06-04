package io.github.youseonghyeon.broadcast;

public interface MessageBroadcaster {

    void broadcast(Long roomId, Object message);

}
