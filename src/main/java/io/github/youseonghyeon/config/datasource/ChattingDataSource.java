package io.github.youseonghyeon.config.datasource;

import java.net.Socket;
import java.util.Set;

public interface ChattingDataSource {

    Set<Socket> registerSocketToRoom(Socket socket, Long roomId);

    void removeSocketFromRoom(Socket socket, Long roomId);

    Set<Socket> findRoomBy(Long roomId);
}
