package io.github.youseonghyeon.session;

import java.net.Socket;
import java.util.List;
import java.util.Set;

@Deprecated(forRemoval = true)
public interface ChatRoomLegacy {

    /**
     * 주어진 소켓을 지정된 채팅방에 등록합니다.
     *
     * @param socket 등록할 소켓
     * @param roomId 소켓이 참여할 채팅방 ID
     */
    void join(Socket socket, Long roomId);

    /**
     * 주어진 소켓을 지정된 채팅방에서 제거합니다.
     *
     * @param socket 제거할 소켓
     * @param roomId 채팅방 ID
     */
    void leave(Socket socket, Long roomId);

    /**
     * 채팅방에 등록된 모든 소켓을 반환합니다.
     */
    List<Socket> getAllSessions();

    /**
     * 특정 채팅방에 등록된 모든 소켓을 반환합니다.
     * 채팅방에 등록된 소켓이 없는 경우 빈 Set을 반환합니다.
     *
     * @param roomId 채팅방 ID
     * @return 해당 채팅방의 소켓 목록
     */
    Set<Socket> findRoomBy(Long roomId);

}
