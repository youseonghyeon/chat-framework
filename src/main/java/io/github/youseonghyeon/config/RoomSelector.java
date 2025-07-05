package io.github.youseonghyeon.config;

import java.net.Socket;

@FunctionalInterface
public interface RoomSelector<T> {

    long selectRoom(Socket socket, T context);
}
