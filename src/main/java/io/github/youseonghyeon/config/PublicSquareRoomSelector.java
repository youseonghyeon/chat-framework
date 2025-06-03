package io.github.youseonghyeon.config;

import java.net.Socket;

public class PublicSquareRoomSelector<T> implements RoomSelector<T> {

    @Override
    public long selectRoom(Socket socket, T context) {
        return 0;
    }
}
