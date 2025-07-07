package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.dto.SendResult;
import io.github.youseonghyeon.core.exception.InvalidChatRoomConfigException;
import io.github.youseonghyeon.core.exception.InvalidMessageException;
import io.github.youseonghyeon.core.exception.UserNotConnectedException;
import io.github.youseonghyeon.core.exception.UserSessionInvalidException;
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

/**
 * ChatRoom represents a logical room where multiple {@link User} instances can join,
 * leave, and exchange {@link Message} objects through a {@link SocketChannel}.
 *
 * <p>This class ensures thread-safe access to internal state and supports broadcasting
 * messages with filtering capability. It also provides basic cleanup logic and
 * participant introspection.</p>
 *
 * @see User
 * @see MessageSender
 * @see Message
 */
public class ChatRoom {

    private static final Logger log = LoggerFactory.getLogger(ChatRoom.class);

    private final String roomId;
    private final LocalDateTime createTime = LocalDateTime.now();
    private final Set<User> participants = ConcurrentHashMap.newKeySet();
    private final MessageSender messageSender;
    private ReentrantLock roomLock = new ReentrantLock();
    private SendFilterPolicy sendFilterPolicy;

    /**
     * Constructs a new chat room with a given identifier and message sender.
     *
     * @param roomId        the unique identifier of the chat room
     * @param messageSender the message delivery mechanism to be used
     * @throws InvalidChatRoomConfigException if the roomId is null/blank or messageSender is null
     */
    public ChatRoom(String roomId, MessageSender messageSender) {
        if (!StringUtils.hasText(roomId) || messageSender == null) {
            throw new InvalidChatRoomConfigException("Chat room ID and message sender cannot be null or empty");
        }
        this.roomId = roomId;
        this.messageSender = messageSender;
    }

    /**
     * Adds a user to the chat room's participant list.
     *
     * @param user the user to join
     * @return true if the user was successfully added, false if they were already a participant
     * @throws UserSessionInvalidException if the user or their socket channel is null
     * @throws UserNotConnectedException   if the socket channel is not currently connected
     */
    public boolean join(User user) {
        if (user == null || user.getSocketChannel() == null)
            throw new UserSessionInvalidException("User or socket channel is null");
        if (!user.getSocketChannel().isConnected())
            throw new UserNotConnectedException("User socket channel is not connected");
        if (participants.contains(user)) {
            log.warn("User {} is already in room {}", user.getSocketChannel(), roomId);
            return false;
        }

        return LockCoordinator.withLock(() -> participants.add(user), roomLock, 5);
    }

    /**
     * Removes a user from the chat room. The underlying socket channel is not closed,
     * assuming a user may participate in multiple rooms.
     *
     * @param user the user to remove
     * @throws UserSessionInvalidException if user or socket channel is null
     */
    public boolean leave(User user) {
        if (user == null || user.getSocketChannel() == null)
            throw new UserSessionInvalidException("User or socket channel is null");

        // socket channel must be connected to leave
        return LockCoordinator.withLock(() -> participants.remove(user), roomLock, 5);
    }

    /**
     * Removes a user from the chat room based on their {@link SocketChannel} reference.
     * If no such user exists, this is a no-op.
     *
     * @param socketChannel the channel identifying the user session
     * @throws UserSessionInvalidException if the channel is null
     */
    public boolean leave(SocketChannel socketChannel) {
        if (socketChannel == null)
            throw new UserSessionInvalidException("Socket channel is null");

        return participants.stream()
                .filter(user -> user.getSocketChannel().equals(socketChannel))
                .findFirst()
                .map(this::leave)
                .orElse(false);
    }

    /**
     * Broadcasts a message to all participants except the optional sender.
     *
     * @param message the message to send
     * @param sender  the sender's socket channel, may be null
     * @throws InvalidMessageException if the message is null
     */
    public void broadcast(Message message, @Nullable SocketChannel sender) {
        if (message == null) {
            throw new InvalidMessageException("Null message cannot be broadcasted");
        }
        // TODO filterPolicy는 engine config 에서 가져오는 것으로 변경 필요
        Predicate<User> filterPolicy = user -> !user.getSocketChannel().equals(sender);

        participants.stream()
                .filter(filterPolicy)
                .forEach(user -> sendMessage(user.getSocketChannel(), message));

        // TODO Result 반환하도록 변경 필요
    }

    /**
     * Sends a message to a specific socket channel.
     *
     * @param client  the recipient's socket channel
     * @param message the message to be delivered
     * @return the result of the send operation (currently not implemented)
     */
    private SendResult sendMessage(SocketChannel client, Message message) {
        try {
            messageSender.send(client, message);
            return SendResult.emptyResult(); // Unsupported operation
        } catch (IOException e) {
            log.error("Failed to send message to client: {}", client, e);
            return SendResult.emptyResult(); // Unsupported operation
        }
    }

    /**
     * Removes all participants whose socket connections are no longer open.
     * Intended to be invoked periodically by an external reaper thread.
     */
    public void sweepParticipants() {
        participants.removeIf(user -> {
            try {
                return !user.getSocketChannel().isOpen();
            } catch (Exception e) {
                log.warn("Failed to check if user socket channel is open: {}", user, e);
                return true; // 예외 발생 시 해당 유저 제거
            }
        });
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    public String getRoomId() {
        return roomId;
    }

    public Set<User> getParticipants() {
        return participants;
    }
}
