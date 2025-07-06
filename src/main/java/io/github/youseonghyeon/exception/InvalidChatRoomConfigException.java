package io.github.youseonghyeon.exception;

public class InvalidChatRoomConfigException extends InitChatResourceException {
    public InvalidChatRoomConfigException() {
        super();
    }

    public InvalidChatRoomConfigException(String message) {
        super(message);
    }

    public InvalidChatRoomConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidChatRoomConfigException(Throwable cause) {
        super(cause);
    }
}
