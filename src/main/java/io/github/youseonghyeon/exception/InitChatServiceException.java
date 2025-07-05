package io.github.youseonghyeon.exception;

import java.io.IOException;

public class InitChatServiceException extends RuntimeException {
    public InitChatServiceException(IOException e) {
    }
}
