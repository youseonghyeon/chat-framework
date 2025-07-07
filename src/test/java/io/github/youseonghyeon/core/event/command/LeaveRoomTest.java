package io.github.youseonghyeon.core.event.command;

import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import org.junit.jupiter.api.Test;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class LeaveRoomTest {

    @Test
    void subscribe_ShouldCallLeaveOnChatRoom_WhenChatRoomExists() {
        // Arrange
        String roomId = "room1";
        SocketChannel socketChannel = mock(SocketChannel.class);
        ChatRoom chatRoom = mock(ChatRoom.class);

        Map<String, ChatRoom> chatRoomMap = new HashMap<>();
        chatRoomMap.put(roomId, chatRoom);

        LeaveRoom leaveRoom = new LeaveRoom(chatRoomMap);

        Message message = new Message(EventType.LEAVE, roomId, null, null, socketChannel);

        // Act
        leaveRoom.subscribe(message);

        // Assert
        verify(chatRoom, times(1)).leave(socketChannel);
    }

    @Test
    void subscribe_ShouldThrowIllegalStateException_WhenChatRoomDoesNotExist() {
        // Arrange
        String roomId = "nonExistentRoom";
        SocketChannel socketChannel = mock(SocketChannel.class);

        Map<String, ChatRoom> chatRoomMap = new HashMap<>();

        LeaveRoom leaveRoom = new LeaveRoom(chatRoomMap);

        Message message = new Message(EventType.LEAVE, roomId, null, null, socketChannel);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> leaveRoom.subscribe(message));
        assertEquals("Chat room not found: " + roomId, exception.getMessage());
    }
}
