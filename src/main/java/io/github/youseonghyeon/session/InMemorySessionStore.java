package io.github.youseonghyeon.session;

import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class InMemorySessionStore {

    private final Map<Long, Set<Socket>> roomSockets;

    public InMemorySessionStore enableReverseLookup() {
        return new InvertedIndexSessionStore(roomSockets);
    }

    public InMemorySessionStore() {
        this.roomSockets = new ConcurrentHashMap<>();
    }

    protected InMemorySessionStore(Map<Long, Set<Socket>> roomSockets) {
        this.roomSockets = roomSockets;
    }

    /**
     * 주어진 소켓을 지정된 채팅방에 등록합니다.
     *
     * @param socket 등록할 소켓
     * @param roomId 소켓이 참여할 채팅방 ID
     */
    public void registerSocketToRoom(Socket socket, Long roomId) {
        this.addToRoom(socket, roomId);
        roomSockets.get(roomId); // side-effect 없음, 필요 없으면 제거 가능
    }

    /**
     * 주어진 소켓을 지정된 채팅방에서 제거합니다.
     *
     * @param socket 제거할 소켓
     * @param roomId 채팅방 ID
     */
    public void removeSocketFromRoom(Socket socket, Long roomId) {
        this.removeFromRoom(socket, roomId);
    }

    /**
     * 특정 채팅방에 등록된 모든 소켓을 반환합니다.
     * <p>
     * 채팅방에 등록된 소켓이 없는 경우 빈 Set을 반환합니다.
     * </p>
     *
     * @param roomId 채팅방 ID
     * @return 해당 채팅방의 소켓 목록
     */
    public Set<Socket> findRoomBy(Long roomId) {
        return roomSockets.getOrDefault(roomId, Set.of());
    }

    /**
     * 소켓을 채팅방에 추가하고, 필요 시 역방향 인덱스도 갱신합니다.
     */
    private void addToRoom(Socket socket, Long roomId) {
        roomSockets
                .computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>())
                .add(socket);
    }

    /**
     * 소켓을 채팅방에서 제거하고, 필요 시 역방향 인덱스도 갱신합니다.
     */
    private void removeFromRoom(Socket socket, Long roomId) {
        roomSockets.computeIfPresent(roomId, (id, sockSet) -> {
            sockSet.remove(socket);
            return sockSet.isEmpty() ? null : sockSet;
        });
    }

    class InvertedIndexSessionStore extends InMemorySessionStore {

        private final Map<Socket, Set<Long>> invertedIndex = new ConcurrentHashMap<>();

        public InvertedIndexSessionStore(Map<Long, Set<Socket>> roomSockets) {
            super(roomSockets);
            roomSockets.forEach((roomId, sockets) -> {
                sockets.forEach(socket -> addInvertedIndex(socket, roomId));
            });
        }

        @Override
        public InMemorySessionStore enableReverseLookup() {
            return this;
        }

        @Override
        public void registerSocketToRoom(Socket socket, Long roomId) {
            super.registerSocketToRoom(socket, roomId);
            addInvertedIndex(socket, roomId);
        }

        @Override
        public void removeSocketFromRoom(Socket socket, Long roomId) {
            super.removeSocketFromRoom(socket, roomId);
            removeInvertedIndex(socket, roomId);
        }

        private void addInvertedIndex(Socket socket, Long roomId) {
            invertedIndex
                    .computeIfAbsent(socket, k -> new CopyOnWriteArraySet<>())
                    .add(roomId);
        }

        private void removeInvertedIndex(Socket socket, Long roomId) {
            invertedIndex.computeIfPresent(socket, (sock, roomIds) -> {
                roomIds.remove(roomId);
                return roomIds.isEmpty() ? null : roomIds;
            });
        }

    }

}
