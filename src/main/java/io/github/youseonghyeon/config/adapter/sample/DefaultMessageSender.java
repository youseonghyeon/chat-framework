package io.github.youseonghyeon.config.adapter.sample;

import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.dto.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class DefaultMessageSender implements MessageSender {

    @Override
    public void send(SocketChannel channel, Message message) throws IOException {
        ByteBuffer buffer = serialize(message);
        buffer.flip(); // 쓰기 완료 → 읽기 모드 전환
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private ByteBuffer serialize(Message message) {
        byte[] roomIdBytes = message.roomId().getBytes(StandardCharsets.UTF_8);
        byte[] header = message.header();
        byte[] content = message.content();

        int totalSize =
                4 + // eventType ordinal (int)
                4 + roomIdBytes.length + // roomId length + data
                4 + header.length + // header length + data
                4 + content.length; // content length + data

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        // 1. EventType ordinal
        buffer.putInt(message.eventType().ordinal());

        // 2. Room ID
        buffer.putInt(roomIdBytes.length);
        buffer.put(roomIdBytes);

        // 3. Header
        buffer.putInt(header.length);
        buffer.put(header);

        // 4. Content
        buffer.putInt(content.length);
        buffer.put(content);

        return buffer;
    }
}
