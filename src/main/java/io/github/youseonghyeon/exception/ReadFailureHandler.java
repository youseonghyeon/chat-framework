package io.github.youseonghyeon.exception;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface ReadFailureHandler {
    void handle(SocketChannel channel, ChannelReadException e);
}
