package io.github.youseonghyeon;

import java.io.OutputStream;

@FunctionalInterface
public interface SendMessage {

    void send(OutputStream outputStream);
}
