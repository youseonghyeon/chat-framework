package io.github.youseonghyeon;

import io.github.youseonghyeon.engine.ChatManager;
import io.github.youseonghyeon.engine.ChattingEngine;
import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.DefaultMessageWriter;
import io.github.youseonghyeon.engine.config.PublicSquareRoomSelector;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EngineStarter {

    public static void main(String[] args) {
        ChattingEngine engine = new ChattingEngine();

        engine.setConfig(config -> config
                .roomSelector(new PublicSquareRoomSelector<>())
                .sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf())
                .threadPool(10, 100)
        );

        engine.start();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new ServerSocketExample(engine));
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdownNow));
    }

    private static class HandleClient implements Runnable {
        private final Socket socket;
        private final ChatManager chatManager;
        private final Long roomId;

        public HandleClient(Socket socket, ChatManager chatManager, Long roomId) {
            this.socket = socket;
            this.chatManager = chatManager;
            this.roomId = roomId;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                while (true) {
                    String message = dis.readUTF();
                    chatManager.send(socket, roomId, message, new DefaultMessageWriter(null));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public static class ServerSocketExample implements Callable<Void> {

        private final ChattingEngine chattingEngine;

        public ServerSocketExample(ChattingEngine chattingEngine) {
            this.chattingEngine = chattingEngine;
        }

        @Override
        public Void call() throws Exception {
            try (ServerSocket serverSocket = new ServerSocket(9999)) {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress() + ":" + socket.getPort());
                    long roomId = chattingEngine.participate(socket, null);
                    HandleClient handleClient = new HandleClient(socket, chattingEngine.chatManager(), roomId);
                    Thread.startVirtualThread(handleClient);
                }
            }
        }
    }


}
