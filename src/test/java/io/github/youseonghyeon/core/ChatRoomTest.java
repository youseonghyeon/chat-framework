package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.exception.UserNotConnectedException;
import io.github.youseonghyeon.core.exception.UserSessionInvalidException;
import io.github.youseonghyeon.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ChatRoomTest {

    private ChatRoom chatRoom;

    @Mock
    private SocketChannel mockSocketChannel;

    @Mock
    private User mockUser;

    @Mock
    private MessageSender mockMessageSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatRoom = new ChatRoom("roomId", mockMessageSender); // Pass a simple constructor fitting your ChatRoom constructor.
    }

    @Test
    @DisplayName("채팅룸 join 성공 테스트")
    void joinShouldAddUserSuccessfully() {
        when(mockUser.getSocketChannel()).thenReturn(mockSocketChannel);
        when(mockSocketChannel.isConnected()).thenReturn(true);

        boolean result = chatRoom.join(mockUser);
        assertTrue(result, "User should be added successfully");
    }

    @Test
    @DisplayName("채팅룸 join 실패 테스트 - null User")
    void joinShouldThrowExceptionForNullUser() {
        assertThrows(UserSessionInvalidException.class, () -> chatRoom.join(null),
                "Expected exception for null user");
    }

    @Test
    @DisplayName("채팅룸 join 실패 테스트 - null SocketChannel")
    void joinShouldThrowExceptionForNullSocketChannel() {
        when(mockUser.getSocketChannel()).thenReturn(null);

        assertThrows(UserSessionInvalidException.class, () -> chatRoom.join(mockUser),
                "Expected exception for null socket channel");
    }

    @Test
    @DisplayName("채팅룸 join 실패 테스트 - User의 SocketChannel이 연결되지 않은 경우")
    void joinShouldThrowExceptionIfSocketChannelNotConnected() {
        when(mockUser.getSocketChannel()).thenReturn(mockSocketChannel);
        when(mockSocketChannel.isConnected()).thenReturn(false);

        assertThrows(UserNotConnectedException.class, () -> chatRoom.join(mockUser),
                "Expected exception for non-connected socket channel");
    }

    @Test
    @DisplayName("채팅룸 join 실패 테스트 - 이미 참여 중인 User")
    void joinShouldPreventDuplicateUserJoining() {
        when(mockUser.getSocketChannel()).thenReturn(mockSocketChannel);
        when(mockSocketChannel.isConnected()).thenReturn(true);

        chatRoom.join(mockUser);
        boolean result = chatRoom.join(mockUser);

        assertFalse(result, "User should not be allowed to join twice");
    }
}
