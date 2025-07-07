package io.github.youseonghyeon.core.exception;

public class ClientConnectionException extends RuntimeException {

    public ClientConnectionException() {
        super();
    }

    public ClientConnectionException(String message) {
        super(message);
    }

    public ClientConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientConnectionException(Throwable cause) {
        super(cause);
    }
}
