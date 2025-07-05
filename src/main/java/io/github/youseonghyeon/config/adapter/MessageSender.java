package io.github.youseonghyeon.config.adapter;

import io.github.youseonghyeon.core.dto.Message;

import java.io.IOException;
import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface MessageSender {
    void send(SocketChannel channel, Message message) throws IOException;
}
