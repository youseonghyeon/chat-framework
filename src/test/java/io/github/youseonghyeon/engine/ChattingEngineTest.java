package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.engine.config.RoomSelector;
import org.junit.jupiter.api.Test;

import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ChattingEngineTest {


    @Test
    void testParticipate_ValidContextAndSocket_AssignsRoom() {
        // Arrange
        ChattingEngine chattingEngine = new ChattingEngine();
        RoomSelector<String> mockRoomSelector = mock(RoomSelector.class);

        chattingEngine.setConfig(config -> {
            config.roomSelector(mockRoomSelector);
            return config;
        });

        chattingEngine.initDefaultConfigIfAbsent();
        chattingEngine.initResource();
        chattingEngine.initThreadPool();

        Socket mockSocket = mock(Socket.class);
        String mockContext = "TestContext";
        long expectedRoomId = 123L;

        when(mockRoomSelector.selectRoom(mockSocket, mockContext)).thenReturn(expectedRoomId);

        // Act
        long roomId = chattingEngine.participate(mockSocket, mockContext);

        // Assert
        assertEquals(expectedRoomId, roomId);
        verify(mockRoomSelector, times(1)).selectRoom(mockSocket, mockContext);

        chattingEngine.stopThreadPool();
    }

    @Test
    void testParticipate_NullContext_ThrowsException() {
        // Arrange
        ChattingEngine chattingEngine = new ChattingEngine();
        ChattingEngineConfig mockConfig = mock(ChattingEngineConfig.class);
        RoomSelector<String> mockRoomSelector = mock(RoomSelector.class);

        chattingEngine.setConfig(config -> {
            config.roomSelector(mockRoomSelector);
            return config;
        });

        chattingEngine.initDefaultConfigIfAbsent();
        chattingEngine.initResource();
        chattingEngine.initThreadPool();

        Socket mockSocket = mock(Socket.class);

        // Act & Assert
        try {
            chattingEngine.participate(mockSocket, null);
        } catch (NullPointerException e) {
            assertEquals("Context cannot be null", e.getMessage());
        }

        chattingEngine.stopThreadPool();
    }

    @Test
    void testParticipate_InvalidRoomSelector_ThrowsException() {
        // Arrange
        ChattingEngine chattingEngine = new ChattingEngine();
        ChattingEngineConfig mockConfig = new ChattingEngineConfig();

        chattingEngine.setConfig(config ->
            config.roomSelector(new PublicSquareRoomSelector<>())
        );

        chattingEngine.initDefaultConfigIfAbsent();
        chattingEngine.initResource();
        chattingEngine.initThreadPool();

        Socket mockSocket = mock(Socket.class);
        String mockContext = "TestContext";

        // Act & Assert
        try {
            chattingEngine.participate(mockSocket, mockContext);
        } catch (NullPointerException e) {
            assertEquals("RoomSelector is not configured", e.getMessage());
        }

        chattingEngine.stopThreadPool();
    }
}
