package io.github.youseonghyeon.core.exception;

public class ChannelReadException extends RuntimeException {
    public ChannelReadException() {
        super();
    }

    public ChannelReadException(String message) {
        super(message);
    }

    public ChannelReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelReadException(Throwable cause) {
        super(cause);
    }
}
