package io.github.youseonghyeon.config.adapter.sample;

import io.github.youseonghyeon.config.adapter.MessageReceiver;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.exception.ChannelWriteException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class DefaultMessageReceiver implements MessageReceiver {

    @Override
    public Message read(SocketChannel channel) throws ChannelWriteException {
        ByteBuffer intBuffer = ByteBuffer.allocate(4);

        // 1. Read eventType ordinal
        readFully(channel, intBuffer);
        intBuffer.flip();
        int eventTypeOrdinal = intBuffer.get();
        EventType eventType = EventType.values()[eventTypeOrdinal];
        intBuffer.clear();

        // 2. Read roomId
        readFully(channel, intBuffer);
        intBuffer.flip();
        int roomIdLength = intBuffer.getInt();
        ByteBuffer roomIdBuffer = ByteBuffer.allocate(roomIdLength);
        readFully(channel, roomIdBuffer);
        roomIdBuffer.flip();
        String roomId = StandardCharsets.UTF_8.decode(roomIdBuffer).toString();
        intBuffer.clear();

        // 3. Read header
        readFully(channel, intBuffer);
        intBuffer.flip();
        int headerLength = intBuffer.getInt();
        ByteBuffer headerBuffer = ByteBuffer.allocate(headerLength);
        readFully(channel, headerBuffer);
        headerBuffer.flip();
        byte[] header = new byte[headerLength];
        headerBuffer.get(header);
        intBuffer.clear();

        // 4. Read content
        readFully(channel, intBuffer);
        intBuffer.flip();
        int contentLength = intBuffer.getInt();
        ByteBuffer contentBuffer = ByteBuffer.allocate(contentLength);
        readFully(channel, contentBuffer);
        contentBuffer.flip();
        byte[] content = new byte[contentLength];
        contentBuffer.get(content);

        return new Message(eventType, roomId, header, content, channel);
    }

    private static void readFully(SocketChannel channel, ByteBuffer buffer) {
        try {
            while (buffer.hasRemaining()) {
                int read = channel.read(buffer);
                if (read == -1) throw new EOFException("Channel closed while reading");
            }
        } catch (IOException e) {
            throw new ChannelWriteException(e);
        }

    }
}
