package io.github.youseonghyeon.session;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@code SessionStore}는 채팅방 ID와 소켓 간의 매핑을 저장하는 기본 세션 저장소입니다.
 * <p>
 * 채팅방 내 클라이언트 소켓들을 관리하며, 기본적으로 단방향(roomId → sockets) 인덱스를 제공합니다.
 * 소켓을 채팅방에 등록하거나 제거할 수 있으며, 특정 채팅방에 속한 모든 소켓 목록을 조회할 수 있습니다.
 * </p>
 *
 * <p>
 * 역방향 인덱스(socket → roomIds)를 필요로 할 경우 {@link #enableReverseLookup()}를 호출하여
 * {@code InvertedIndexSessionStore} 구현체를 사용할 수 있습니다.
 * </p>
 *
 * <h3>예시</h3>
 * <pre>{@code
 * SessionStore store = new SessionStore().enableReverseLookup();
 * store.registerSocketToRoom(socket, 1L);
 * }</pre>
 */
public class SessionStore {
    // TODO 끊어진 세션 제거용 스레드를 추가하거나, 조회할 때 같이 제거하는 로직 추가 필요

    /**
     * roomId → 참여 소켓 집합을 저장하는 맵
     */
    private final Map<Long, Set<Socket>> roomSockets;

    /**
     * 역방향 인덱싱 기능이 필요한 경우 {@link InvertedIndexSessionStore}로 변환합니다.
     *
     * @return 역방향 인덱스를 지원하는 {@code SessionStore} 구현체
     */
    public SessionStore enableReverseLookup() {
        return new InvertedIndexSessionStore(roomSockets);
    }

    /**
     * 기본 생성자. 비어 있는 저장소로 초기화합니다.
     */
    public SessionStore() {
        this.roomSockets = new ConcurrentHashMap<>();
    }

    /**
     * 내부용 생성자. 주어진 roomSockets 맵을 공유하여 사용하는 구현체 생성을 허용합니다.
     */
    protected SessionStore(Map<Long, Set<Socket>> roomSockets) {
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
        roomSockets.get(roomId); // Optional: side-effect 없음
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
     * 채팅방에 등록된 모든 소켓을 반환합니다.
     */
    public List<Socket> getAllSessions() {
        return roomSockets.values().stream().flatMap(Set::stream).toList();
    }

    /**
     * 특정 채팅방에 등록된 모든 소켓을 반환합니다.
     * 채팅방에 등록된 소켓이 없는 경우 빈 Set을 반환합니다.
     *
     * @param roomId 채팅방 ID
     * @return 해당 채팅방의 소켓 목록
     */
    public Set<Socket> findRoomBy(Long roomId) {
        return roomSockets.getOrDefault(roomId, Set.of());
    }

    // 내부 구현 메서드
    private void addToRoom(Socket socket, Long roomId) {
        roomSockets
                .computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>())
                .add(socket);
    }

    private void removeFromRoom(Socket socket, Long roomId) {
        roomSockets.computeIfPresent(roomId, (id, sockSet) -> {
            sockSet.remove(socket);
            return sockSet.isEmpty() ? null : sockSet;
        });
    }

    /**
     * {@code InvertedIndexSessionStore}는 {@link SessionStore}의 확장 구현으로,
     * socket → roomId의 역방향 인덱스를 함께 유지합니다.
     * <p>
     * 이 구현은 소켓 기준으로 참여 중인 채팅방을 조회하거나 관리해야 할 경우 유용합니다.
     * </p>
     */
    class InvertedIndexSessionStore extends SessionStore {

        private final Map<Socket, Set<Long>> invertedIndex = new ConcurrentHashMap<>();

        /**
         * 기존의 roomId → socket 인덱스를 공유하여 초기화합니다.
         * 동시에 역방향 인덱스도 동기화합니다.
         *
         * @param roomSockets 기존 room 인덱스
         */
        public InvertedIndexSessionStore(Map<Long, Set<Socket>> roomSockets) {
            super(roomSockets);
            roomSockets.forEach((roomId, sockets) -> {
                sockets.forEach(socket -> addInvertedIndex(socket, roomId));
            });
        }

        /**
         * 이미 역방향 인덱스를 지원하므로 그대로 반환합니다.
         */
        @Override
        public SessionStore enableReverseLookup() {
            return this;
        }

        /**
         * 소켓을 채팅방에 등록하고, 역방향 인덱스를 갱신합니다.
         */
        @Override
        public void registerSocketToRoom(Socket socket, Long roomId) {
            super.registerSocketToRoom(socket, roomId);
            addInvertedIndex(socket, roomId);
        }

        /**
         * 소켓을 채팅방에서 제거하고, 역방향 인덱스를 갱신합니다.
         */
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
