package io.github.youseonghyeon.model;

import io.github.youseonghyeon.exception.ClientConnectionException;

import java.io.IOException;
import java.nio.channels.SocketChannel;

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
}
