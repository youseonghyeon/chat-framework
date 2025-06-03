package io.github.youseonghyeon;

import io.github.youseonghyeon.config.ChattingEngineConfig;
import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class ChatManger {

    private final ChattingDataSource dataSource;
    private final ThreadPoolExecutor executor;
    private final SendFilterPolicy filterPolicy;

    public ChatManger(ChattingDataSource dataSource, ThreadPoolExecutor executor, ChattingEngineConfig config) {
        this.dataSource = dataSource;
        this.executor = executor;
        this.filterPolicy = config.getSendFilterPolicy();
    }

    public <T> void send(Socket self, Long roomId, T message, SendParser<T> parser) throws NoParticipantException {
        Set<Socket> sockets = dataSource.findRoomBy(roomId);
        List<Socket> targetSockets = sockets.stream().filter(socket -> filterPolicy.shouldSend(socket, self)).toList();
        if (targetSockets.isEmpty()) {
            throw new NoParticipantException("No participants in the room: " + roomId);
        }

        for (Socket socket : targetSockets) {
            executor.execute(() -> {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    parser.write(message, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
