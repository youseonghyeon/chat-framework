package io.github.youseonghyeon.model;

import io.github.youseonghyeon.core.exception.ClientConnectionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserTest {

    /**
     * Test for the closeConnection method in the User class.
     * Ensures that the method properly closes an open SocketChannel and handles exceptions correctly.
     */

    @Test
    @DisplayName("사용자 연결 종료 테스트")
    void testCloseConnection_whenSocketChannelIsOpen_closesSuccessfully() throws IOException {
        // Arrange
        SocketChannel mockSocketChannel = mock(SocketChannel.class);
        when(mockSocketChannel.isOpen()).thenReturn(true);
        User user = new User(mockSocketChannel);

        // Act
        user.closeConnection();

        // Assert
        verify(mockSocketChannel, times(1)).close();
    }

    @Test
    @DisplayName("사용자 연결 종료 테스트 - 소켓 채널이 닫혀있을 때")
    void testCloseConnection_whenSocketChannelIsClosed_noActionTaken() throws IOException {
        // Arrange
        SocketChannel mockSocketChannel = mock(SocketChannel.class);
        when(mockSocketChannel.isOpen()).thenReturn(false);
        User user = new User(mockSocketChannel);

        // Act
        user.closeConnection();

        // Assert
        verify(mockSocketChannel, never()).close();
    }

    @Test
    @DisplayName("사용자 연결 종료 테스트 - 소켓 채널이 null일 때")
    void testCloseConnection_whenSocketChannelThrowsIOException_throwsClientConnectionException() throws IOException {
        // Arrange
        SocketChannel mockSocketChannel = mock(SocketChannel.class);
        when(mockSocketChannel.isOpen()).thenReturn(true);
        doThrow(IOException.class).when(mockSocketChannel).close();
        User user = new User(mockSocketChannel);

        // Act and Assert
        assertThrows(ClientConnectionException.class, user::closeConnection);
    }

    @Test
    @DisplayName("사용자 연결 종료 테스트 - 소켓 채널이 null일 때 예외 없이 종료")
    void testCloseConnection_whenSocketChannelIsNull_noExceptionThrown() {
        // Arrange
        User user = new User(null);

        // Act and Assert
        assertDoesNotThrow(user::closeConnection);
    }
}
