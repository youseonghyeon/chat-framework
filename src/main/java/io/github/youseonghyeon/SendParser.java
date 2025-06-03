package io.github.youseonghyeon;

import java.io.OutputStream;

@FunctionalInterface
public interface SendParser<T> {

    void write(T message, OutputStream outputStream);
}
