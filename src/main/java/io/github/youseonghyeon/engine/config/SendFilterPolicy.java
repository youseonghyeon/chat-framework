package io.github.youseonghyeon.engine.config;

import java.net.Socket;
import java.util.Objects;

@FunctionalInterface
public interface SendFilterPolicy {

    boolean shouldSend(Socket receiver, Socket sender);

    default SendFilterPolicy and(SendFilterPolicy other) {
        Objects.requireNonNull(other);
        return (receiver, sender) ->
                this.shouldSend(receiver, sender)
                && other.shouldSend(receiver, sender);
    }

    default SendFilterPolicy negate() {
        return (receiver, sender) ->
                !this.shouldSend(receiver, sender);
    }

    default SendFilterPolicy or(SendFilterPolicy other) {
        Objects.requireNonNull(other);
        return (receiver, sender) ->
                this.shouldSend(receiver, sender)
                || other.shouldSend(receiver, sender);
    }

    static SendFilterPolicy not(SendFilterPolicy target) {
        Objects.requireNonNull(target);
        return target.negate();
    }
}
