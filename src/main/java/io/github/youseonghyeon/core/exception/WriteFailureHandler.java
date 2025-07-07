package io.github.youseonghyeon.core.exception;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface WriteFailureHandler {
    void handle(SocketChannel channel, ChannelReadException e);
}
