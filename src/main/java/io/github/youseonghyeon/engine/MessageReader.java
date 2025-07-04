package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.dto.Message;

import java.nio.channels.SocketChannel;
import java.util.function.Function;

public class MessageReader implements Function<SocketChannel, Message> {

    @Override
    public Message apply(SocketChannel channel) {
        // TODO create Message reader operator
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
