package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.model.User;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom {

    private final String roomId;
    private final Set<User> participants = ConcurrentHashMap.newKeySet();
    private final MessageSender messageSender;
    private final LocalDateTime createTime = LocalDateTime.now();

    public ChatRoom(String roomId, MessageSender messageSender) {
        this.roomId = roomId;
        this.messageSender = messageSender;
    }

    public void join(User user) {
        participants.add(user);
    }

    public void leave(User user) {
        user.closeConnection();
        participants.remove(user);
    }

    public void leave(SocketChannel socketChannel) {
        participants.stream()
                .filter(user -> user.getSocketChannel().equals(socketChannel))
                .findFirst()
                .ifPresent(this::leave);
    }

    public void broadcast(Message message) {
        broadcast(message, null);
    }

    public void broadcast(Message message, SocketChannel sender) {
        participants.stream().filter(user -> !user.getSocketChannel().equals(sender))
                .forEach(user -> sendMessage(user.getSocketChannel(), message));
    }

    private void sendMessage(SocketChannel client, Message message) {
        try {
            messageSender.send(client, message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
