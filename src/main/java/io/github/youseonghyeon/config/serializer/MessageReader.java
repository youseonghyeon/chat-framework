package io.github.youseonghyeon.config.serializer;

import io.github.youseonghyeon.core.dto.Message;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface MessageReader {
    Message read(SocketChannel channel);
}
