package io.github.youseonghyeon.persistence.impl;

import io.github.youseonghyeon.persistence.dto.ChatLog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryChatHistoryStoreTest {

    /**
     * The `save` method in the `InMemoryChatHistoryStore` class is responsible for storing `ChatLog` objects
     * in a thread-safe manner. Logs are stored based on their associated `roomId`. If the `roomId` does not
     * already exist, a new list is created, and the log is added to it.
     */

    @Test
    void save_shouldAddChatLogToEmptyRoom() {
        InMemoryChatHistoryStore store = new InMemoryChatHistoryStore();
        ChatLog log = new ChatLog(1L, 1000L, "user1", "Hello!");

        store.save(log);

        List<ChatLog> recentLogs = store.findRecentLogs(1L, 1);
        assertEquals(1, recentLogs.size());
        assertEquals(log, recentLogs.get(0));
    }

    @Test
    void save_shouldAddChatLogToExistingRoom() {
        InMemoryChatHistoryStore store = new InMemoryChatHistoryStore();
        ChatLog log1 = new ChatLog(1L, 1000L, "user1", "First message");
        ChatLog log2 = new ChatLog(1L, 2000L, "user2", "Second message");

        store.save(log1);
        store.save(log2);

        List<ChatLog> allLogs = store.findRecentLogs(1L, 10);
        assertEquals(2, allLogs.size());
        assertTrue(allLogs.contains(log1));
        assertTrue(allLogs.contains(log2));
    }

    @Test
    void save_shouldNotAffectOtherRooms() {
        InMemoryChatHistoryStore store = new InMemoryChatHistoryStore();
        ChatLog log1 = new ChatLog(1L, 1000L, "user1", "Message in room 1");
        ChatLog log2 = new ChatLog(2L, 2000L, "user2", "Message in room 2");

        store.save(log1);
        store.save(log2);

        List<ChatLog> logsRoom1 = store.findRecentLogs(1L, 10);
        List<ChatLog> logsRoom2 = store.findRecentLogs(2L, 10);

        assertEquals(1, logsRoom1.size());
        assertEquals(log1, logsRoom1.get(0));

        assertEquals(1, logsRoom2.size());
        assertEquals(log2, logsRoom2.get(0));
    }

    @Test
    void save_shouldAppendLogsToTheSameRoom() {
        InMemoryChatHistoryStore store = new InMemoryChatHistoryStore();
        ChatLog log1 = new ChatLog(1L, 1000L, "user1", "First message in room 1");
        ChatLog log2 = new ChatLog(1L, 2000L, "user2", "Second message in room 1");

        store.save(log1);
        store.save(log2);

        List<ChatLog> allLogs = store.findRecentLogs(1L, 10);
        assertEquals(2, allLogs.size());
        assertEquals(log2, allLogs.get(0)); // Sorted in descending order of timestamp
        assertEquals(log1, allLogs.get(1));
    }
}
