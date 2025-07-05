package io.github.youseonghyeon.core.event.action;

import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;

import java.util.Map;

public class SendMessage implements MessageSubscriber {

    public static final EventType type = EventType.USER_SEND;

    private final Map<String, ChatRoom> chatRoomMap;

    public SendMessage(Map<String, ChatRoom> chatRoomMap) {
        this.chatRoomMap = chatRoomMap;
    }

    @Override
    public void subscribe(Message message) {
        ChatRoom chatRoom = chatRoomMap.get(message.roomId());
        chatRoom.broadcast(message, message.socketChannel());
    }

    @Override
    public void init() {

    }
}
