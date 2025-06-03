package io.github.youseonghyeon;

import io.github.youseonghyeon.engine.ChatManager;
import io.github.youseonghyeon.engine.ChattingEngine;
import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.RoomSelector;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConfigExample {


    public static void main(String[] args) throws IOException {
        ChattingEngine engine = new ChattingEngine();

        engine.setConfig(config -> config
                .roomSelector(new RoomIdentifier())
                .sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf())
                .threadPoolSize(10, 100)
        );

        engine.start();

        ChatManager chatManager = engine.chatManager();

        ServerSocket serverSocket = new ServerSocket(9999);
        Socket socket = serverSocket.accept();
        RoomIdentifier context = new RoomIdentifier();
        long roomId = engine.participate(socket, context);

        chatManager.send(socket, roomId, "Hello, World!", (message, outputStream) -> {
            try {
                new DataOutputStream(outputStream).writeUTF(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });


    }

    public static class RoomIdentifier implements RoomSelector<RoomIdentifier> {
        private Long id;

        @Override
        public long selectRoom(Socket socket, RoomIdentifier context) {
            return context.id;
        }
    }


}
