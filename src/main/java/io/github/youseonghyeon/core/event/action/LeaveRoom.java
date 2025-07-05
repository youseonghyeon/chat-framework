package io.github.youseonghyeon.core.event.action;

import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import io.github.youseonghyeon.model.User;

import java.util.Map;

public class LeaveRoom implements MessageSubscriber {

    public static final EventType type = EventType.LEAVE;

    private final Map<String, ChatRoom> chatRoomMap;

    public LeaveRoom(Map<String, ChatRoom> chatRoomMap) {
        this.chatRoomMap = chatRoomMap;
    }

    @Override
    public void subscribe(Message message) {
        ChatRoom chatRoom = chatRoomMap.get(message.roomId());
        if (chatRoom == null) {
            throw new IllegalStateException("Chat room not found: " + message.roomId());
        }

        chatRoom.leave(message.socketChannel());
    }

    @Override
    public void init() {

    }
}
