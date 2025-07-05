package io.github.youseonghyeon.core;

import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.model.User;

import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ChatRoom {

    private final String roomId;
    private final Set<User> participants = ConcurrentHashMap.newKeySet();
    private final LocalDateTime createTime = LocalDateTime.now();

    // TODO config로 설정 필요
    private BiConsumer<SocketChannel, Message> messageWriter;

    public ChatRoom(String roomId) {
        this.roomId = roomId;
    }

    public void join(User user) {
        participants.add(user);
    }

    public void leave(User user) {
        user.closeConnection();
        participants.remove(user);
    }

    public void broadcast(Message message) {
        broadcast(message, null);
    }

    public void broadcast(Message message, SocketChannel sender) {
        participants.stream().filter(user -> !user.getSocketChannel().equals(sender))
                .forEach(user -> sendMessage(user.getSocketChannel(), message));
    }

    public void sendMessage(SocketChannel client, Message message) {
        messageWriter.accept(client, message);
    }
}
