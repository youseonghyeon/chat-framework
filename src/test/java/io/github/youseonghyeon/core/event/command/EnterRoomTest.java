package io.github.youseonghyeon.core.event.command;

import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.ChatRoom;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class EnterRoomTest {

    @Mock
    MessageSender messageSender = mock(MessageSender.class);
    @Mock
    SocketChannel mockClient = mock(SocketChannel.class);

    @Test
    @DisplayName("채팅룸 입장 이벤트 테스트")
    void enterRoomTest() {
        // given
        Map<String, ChatRoom> chatRoomMap = new HashMap<>();
        EnterRoom enterRoom = new EnterRoom(chatRoomMap, messageSender);
        // when
        when(mockClient.isConnected()).thenReturn(true);
        enterRoom.subscribe(new Message(EventType.ENTER, "newRoom", null, null, mockClient));
        //then
        ChatRoom chatRoom = chatRoomMap.get("newRoom");
        Assertions.assertEquals(1, chatRoom.getParticipants().size(), "채팅룸에 유저가 추가되어야 합니다.");

    }


    @Test
    void subscribeWhenChatRoomExistsAddsUserToExistingRoom() {
        // Arrange
        MessageSender messageSender = mock(MessageSender.class);
        SocketChannel mockSocketChannel = mock(SocketChannel.class);
        String roomId = "testRoom";

        ChatRoom existingChatRoom = mock(ChatRoom.class);
        Map<String, ChatRoom> chatRoomMap = new HashMap<>();
        chatRoomMap.put(roomId, existingChatRoom);

        EnterRoom enterRoom = new EnterRoom(chatRoomMap, messageSender);
        Message message = new Message(EventType.ENTER, roomId, null, null, mockSocketChannel);

        // Act
        enterRoom.subscribe(message);

        // Assert
        verify(existingChatRoom, times(1)).join(any(User.class));
    }

}
