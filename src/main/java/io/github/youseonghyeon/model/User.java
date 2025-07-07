package io.github.youseonghyeon.model;

import io.github.youseonghyeon.core.exception.ClientConnectionException;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;

public class User {

    private String nickname;
    private final SocketChannel socketChannel;

    public User(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public void closeConnection() {
        try {
            if (this.socketChannel != null && this.socketChannel.isOpen())
                this.socketChannel.close();
        } catch (IOException e) {
            throw new ClientConnectionException(e);
        }
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(getSocketChannel(), user.getSocketChannel());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getSocketChannel());
    }
}
