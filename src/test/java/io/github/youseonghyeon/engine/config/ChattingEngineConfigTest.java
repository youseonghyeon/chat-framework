package io.github.youseonghyeon.engine.config;

import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.RoomSelector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChattingEngineConfigTest {

    /**
     * Test case to ensure that the roomSelector method sets the RoomSelector implementation correctly.
     */
    @Test
    public void testRoomSelector() {
        // Arrange
        ChattingEngineConfig chattingEngineConfig = new ChattingEngineConfig();
        RoomSelector<String> mockRoomSelector = (socket, context) -> 1234L;

        // Act
        chattingEngineConfig.roomSelector(mockRoomSelector);
        RoomSelector<String> retrievedRoomSelector = chattingEngineConfig.getRoomSelector();

        // Assert
        assertEquals(mockRoomSelector, retrievedRoomSelector, "The roomSelector implementation was not set correctly");
    }
}
