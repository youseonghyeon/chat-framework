package io.github.youseonghyeon.config.datasource;

import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemoryChattingDataSource implements ChattingDataSource {

    private final Map<Long, Set<Socket>> roomSockets = new HashMap<>();
    // 역색인이 필요한경우 사용
    private final Map<Socket, Set<Long>> invertedIndex = new HashMap<>();

    @Override
    public Set<Socket> registerSocketToRoom(Socket socket, Long roomId) {
        this.addToRoom(socket, roomId);
        return roomSockets.get(roomId);
    }

    @Override
    public void removeSocketFromRoom(Socket socket, Long roomId) {
        this.removeFromRoom(socket, roomId);
    }

    @Override
    public Set<Socket> findRoomBy(Long roomId) {
        return roomSockets.getOrDefault(roomId, Set.of());
    }

    private void addToRoom(Socket socket, Long roomId) {
        roomSockets
                .computeIfAbsent(roomId, k -> new HashSet<>())
                .add(socket);
    }

    private void addInvertedIndex(Socket socket, Long roomId) {
        invertedIndex.computeIfAbsent(socket, k -> new HashSet<>())
                .add(roomId);
    }

    private void removeFromRoom(Socket socket, Long roomId) {
        roomSockets.computeIfPresent(roomId, (id, sock) -> {
            sock.remove(socket);
            return sock.isEmpty() ? null : sock;
        });
    }

    private void removeInvertedIndex(Socket socket, Long roomId) {
        invertedIndex.computeIfPresent(socket, (sock, roomIds) -> {
            roomIds.remove(roomId);
            return roomIds.isEmpty() ? null : roomIds;
        });
    }
}
