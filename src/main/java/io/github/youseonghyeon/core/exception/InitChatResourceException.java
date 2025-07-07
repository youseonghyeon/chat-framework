package io.github.youseonghyeon.core.exception;

public class InitChatResourceException extends RuntimeException {

    public InitChatResourceException() {
        super();
    }

    public InitChatResourceException(String message) {
        super(message);
    }

    public InitChatResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitChatResourceException(Throwable cause) {
        super(cause);
    }
}
