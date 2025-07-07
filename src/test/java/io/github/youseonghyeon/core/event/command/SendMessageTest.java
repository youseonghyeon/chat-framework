package io.github.youseonghyeon.core.event.command;

import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.channels.SocketChannel;
import java.util.Map;

import static org.mockito.Mockito.*;

class SendMessageTest {

    /**
     * Tests for the SendMessage class.
     * The SendMessage class is responsible for broadcasting messages to a chat room
     * provided its roomId matches a room in the chatRoomMap.
     */

    @Test
    void subscribe_ShouldBroadcastMessageToCorrectChatRoom() {
        // Arrange
        ChatRoom mockChatRoom = Mockito.mock(ChatRoom.class);
        SocketChannel mockSocketChannel = Mockito.mock(SocketChannel.class);
        Message message = new Message(EventType.USER_SEND, "room1", new byte[]{}, new byte[]{}, mockSocketChannel);

        Map<String, ChatRoom> chatRoomMap = Map.of("room1", mockChatRoom);
        SendMessage sendMessage = new SendMessage(chatRoomMap);

        // Act
        sendMessage.subscribe(message);

        // Assert
        verify(mockChatRoom, times(1)).broadcast(message, mockSocketChannel);
    }

}
