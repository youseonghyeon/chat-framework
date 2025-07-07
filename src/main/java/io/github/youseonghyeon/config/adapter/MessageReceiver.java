package io.github.youseonghyeon.config.adapter;

import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.exception.ChannelReadException;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface MessageReceiver {
    Message read(SocketChannel channel) throws ChannelReadException;
}
