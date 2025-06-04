package io.github.youseonghyeon.engine.config;

import java.io.OutputStream;

@FunctionalInterface
public interface MessageWriter<T> {

    void write(T message, OutputStream outputStream);
}
