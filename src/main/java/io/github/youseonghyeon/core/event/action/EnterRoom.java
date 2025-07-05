package io.github.youseonghyeon.core.event.action;

import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import io.github.youseonghyeon.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EnterRoom implements MessageSubscriber {

    public static final EventType type = EventType.ENTER;
    private static final Logger log = LoggerFactory.getLogger(EnterRoom.class);

    private final Map<String, ChatRoom> chatRoomMap;

    public EnterRoom(Map<String, ChatRoom> chatRoomMap) {
        this.chatRoomMap = chatRoomMap;
    }

    @Override
    public void subscribe(Message message) {
        ChatRoom chatRoom = chatRoomMap.computeIfAbsent(message.roomId(), ChatRoom::new);
        chatRoom.join(new User(message.socketChannel()));
        log.info("User {} entered room {}", message.socketChannel(), message.roomId());
    }

    @Override
    public void init() {

    }
}
