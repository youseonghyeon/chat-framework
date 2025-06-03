package io.github.youseonghyeon.engine.config;

import java.net.Socket;

@FunctionalInterface
public interface RoomSelector<T> {

    long selectRoom(Socket socket, T context);
}
