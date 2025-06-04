package io.github.youseonghyeon.engine.config;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DefaultMessageWriter implements MessageWriter<String> {

    private final SendFailureHandler<String> failureHandler;

    public DefaultMessageWriter(SendFailureHandler<String> failureHandler) {
        this.failureHandler = failureHandler;
    }

    @Override
    public void write(String message, OutputStream outputStream) {
        try {
            DataOutputStream dos = new DataOutputStream(outputStream);
            dos.writeUTF(message);
        } catch (IOException e) {
            failureHandler.onFailure(message, e);
        }
    }
}
