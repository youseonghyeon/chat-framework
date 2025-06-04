package io.github.youseonghyeon.persistence.impl;

import io.github.youseonghyeon.persistence.ChatHistoryStore;
import io.github.youseonghyeon.persistence.dto.ChatLog;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class InMemoryChatHistoryStore implements ChatHistoryStore {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<ChatLog>> store = new ConcurrentHashMap<>();

    @Override
    public void save(ChatLog log) {
        store.computeIfAbsent(log.roomId(), k -> new CopyOnWriteArrayList<>())
                .add(log);
    }

    @Override
    public List<ChatLog> findRecentLogs(long roomId, int limit) {
        List<ChatLog> logs = store.getOrDefault(roomId, new CopyOnWriteArrayList<>());
        return logs.stream()
                .sorted(Comparator.comparingLong(ChatLog::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatLog> findLogsBetween(long roomId, long fromUnix, long toUnix) {
        return store.getOrDefault(roomId, new CopyOnWriteArrayList<>())
                .stream()
                .filter(log -> log.timestamp() >= fromUnix && log.timestamp() <= toUnix)
                .sorted(Comparator.comparingLong(ChatLog::timestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatLog> findLogsBefore(long roomId, long toUnix, int limit) {
        return store.getOrDefault(roomId, new CopyOnWriteArrayList<>())
                .stream()
                .filter(log -> log.timestamp() < toUnix)
                .sorted(Comparator.comparingLong(ChatLog::timestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
