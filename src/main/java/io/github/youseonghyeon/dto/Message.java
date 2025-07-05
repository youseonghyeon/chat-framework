package io.github.youseonghyeon.dto;

import java.io.Serial;
import java.io.Serializable;
import java.nio.channels.SocketChannel;

public class Message implements Serializable {

    @Serial
    private static final long serialVersionUID = 1135971365917834L;

    private MessageType type;
    private byte[] content;
    private byte[] header;


    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }
}
