package io.github.youseonghyeon.engine.config;

@FunctionalInterface
public interface SendFailureHandler<T> {

    void onFailure(T message, Exception cause);
}
