package io.github.youseonghyeon.core.exception;

public class ChannelWriteException extends RuntimeException {
    public ChannelWriteException() {
        super();
    }

    public ChannelWriteException(String message) {
        super(message);
    }

    public ChannelWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelWriteException(Throwable cause) {
        super(cause);
    }
}
