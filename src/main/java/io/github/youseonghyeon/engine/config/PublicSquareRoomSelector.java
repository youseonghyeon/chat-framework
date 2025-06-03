package io.github.youseonghyeon.engine.config;

import java.net.Socket;

/**
 * 모든 참가자가 동일한 채팅방에 참여하도록 하는 단일 공개 채팅방(Room)을 생성하는 RoomSelector 구현체입니다.
 * 이 구현체는 마치 '광장'과 같은 개념의 전역 채팅방을 의미하며, 연결된 모든 사용자가 서로 소통할 수 있습니다.
 *
 * <p>이 RoomSelector는 항상 방 ID 0을 반환하므로, 소켓 연결 정보나 컨텍스트와 관계없이
 * 모든 참가자가 동일한 방에 배치됩니다. 따라서 모든 사용자가 함께 대화할 수 있는
 * 공용 공간(예: 광장)이 형성됩니다.</p>
 */
public class PublicSquareRoomSelector<T> implements RoomSelector<T> {

    @Override
    public long selectRoom(Socket socket, T context) {
        return 0;
    }
}
