package io.github.youseonghyeon.core.maintenance;

import io.github.youseonghyeon.core.ChatRoom;

import java.util.Map;

/// 스케줄링 스레드로 작동 필요
public class ExpirationReaper {

    private final Map<String, ChatRoom> chatRoomMap;

    public ExpirationReaper(Map<String, ChatRoom> chatRoomMap) {
        this.chatRoomMap = chatRoomMap;
    }

    private void cleanup() {
        for (String s : chatRoomMap.keySet()) {
            ChatRoom chatRoom = chatRoomMap.get(s);
            chatRoom.sweepParticipants();
            if (chatRoom.isEmpty()) {
                chatRoomMap.remove(s);
            }
        }
    }
}
