package io.github.youseonghyeon;

import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.EventType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Android {

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 9999));
        Thread.startVirtualThread(new RequestHandler(socketChannel));
        Thread.startVirtualThread(new ResponseHandler(socketChannel));
        Thread.sleep(Long.MAX_VALUE);
    }

    private static class RequestHandler implements Runnable {
        private final SocketChannel socketChannel;
        private final Scanner sc = new Scanner(System.in);

        public RequestHandler(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = sc.nextLine();
                    Message room1 = new Message(EventType.ENTER, "room1", "header".getBytes(StandardCharsets.UTF_8), message.getBytes(StandardCharsets.UTF_8), null);
                    ByteBuffer buffer = serialize(room1);
                    buffer.flip();
                    socketChannel.write(buffer);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static class ResponseHandler implements Runnable {
        private final SocketChannel socketChannel;

        public ResponseHandler(SocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @Override
        public void run() {
//            try {
//                InputStream in = socket.getInputStream();
//                DataInputStream dis = new DataInputStream(in);
//                while (true) {
//                    String message = dis.readUTF();
//                    System.out.println("Received: " + message);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    private static ByteBuffer serialize(Message message) {
        byte[] roomIdBytes = message.roomId().getBytes(StandardCharsets.UTF_8);
        byte[] header = message.header();
        byte[] content = message.content();

        int totalSize =
                4 + // eventType ordinal (int)
                4 + roomIdBytes.length + // roomId length + data
                4 + header.length + // header length + data
                4 + content.length; // content length + data

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        // 1. EventType ordinal
        buffer.putInt(message.eventType().ordinal());

        // 2. Room ID
        buffer.putInt(roomIdBytes.length);
        buffer.put(roomIdBytes);

        // 3. Header
        buffer.putInt(header.length);
        buffer.put(header);

        // 4. Content
        buffer.putInt(content.length);
        buffer.put(content);

        return buffer;
    }
}
