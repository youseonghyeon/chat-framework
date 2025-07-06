package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.dto.SendResult;
import io.github.youseonghyeon.exception.InvalidChatRoomConfigException;
import io.github.youseonghyeon.exception.InvalidMessageException;
import io.github.youseonghyeon.exception.UserNotConnectedException;
import io.github.youseonghyeon.exception.UserSessionInvalidException;
import io.github.youseonghyeon.model.User;
import io.github.youseonghyeon.utils.LockCoordinator;
import io.github.youseonghyeon.utils.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class ChatRoom {

    private static final Logger log = LoggerFactory.getLogger(ChatRoom.class);

    private final String roomId;
    private final LocalDateTime createTime = LocalDateTime.now();
    private final Set<User> participants = ConcurrentHashMap.newKeySet();
    private final MessageSender messageSender;

    private ReentrantLock lock = new ReentrantLock();

    private SendFilterPolicy sendFilterPolicy;

    public ChatRoom(String roomId, MessageSender messageSender) {
        if (!StringUtils.hasText(roomId) || messageSender == null) {
            throw new InvalidChatRoomConfigException("Chat room ID and message sender cannot be null or empty");
        }
        this.roomId = roomId;
        this.messageSender = messageSender;
    }

    public void join(User user) {
        if (user == null || user.getSocketChannel() == null)
            throw new UserSessionInvalidException("User or socket channel is null");
        if (!user.getSocketChannel().isConnected())
            throw new UserNotConnectedException("User socket channel is not connected");

        LockCoordinator.withLock(() -> participants.add(user), lock, 5);
    }

    public void leave(User user) {
        if (user == null || user.getSocketChannel() == null)
            throw new UserSessionInvalidException("User or socket channel is null");

        // 한명의 사용자가 2개의 채팅방을 사용하고 있는 경우가 있으므로, socketChannel은 close 하지 않도록 함
        Boolean leaved = LockCoordinator.withLock(() -> participants.remove(user), lock, 5);

        if (leaved) {
            log.info("User {} left room {}", user.getSocketChannel(), roomId);
        }
    }

    public void leave(SocketChannel socketChannel) {
        if (socketChannel == null)
            throw new UserSessionInvalidException("Socket channel is null");

        participants.stream()
                .filter(user -> user.getSocketChannel().equals(socketChannel))
                .findFirst()
                .ifPresent(this::leave);
    }

    public void broadcast(Message message, @Nullable SocketChannel sender) {
        if (message == null) {
            throw new InvalidMessageException("Null message cannot be broadcasted");
        }
        // TODO filterPolicy는 engine config 에서 가져오는 것으로 변경 필요
        Predicate<User> filterPolicy = user -> !user.getSocketChannel().equals(sender);

        participants.stream()
                .filter(filterPolicy)
                .forEach(user -> sendMessage(user.getSocketChannel(), message));
    }

    private SendResult sendMessage(SocketChannel client, Message message) {
        try {
            messageSender.send(client, message);
            return SendResult.emptyResult(); // 기능 미구현
        } catch (IOException e) {
            log.error("Failed to send message to client: {}", client, e);
            return SendResult.emptyResult(); // 기능 미구현
        }
    }

    protected void sweepParticipants() {
        participants.removeIf(user -> {
            try {
                return !user.getSocketChannel().isOpen();
            } catch (Exception e) {
                log.warn("Failed to check if user socket channel is open: {}", user, e);
                return true; // 예외 발생 시 해당 유저 제거
            }
        });
    }

    public long getParticipantCount() {
        return participants.size();
    }

    public String getRoomId() {
        return roomId;
    }
}
