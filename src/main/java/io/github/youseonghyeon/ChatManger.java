package io.github.youseonghyeon;

import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class ChatManger {

    private final ChattingDataSource dataSource;
    private final ThreadPoolExecutor executor;
    private final SendFilterPolicy filterPolicy;

    public ChatManger(ChattingDataSource dataSource, ThreadPoolExecutor executor, SendFilterPolicy... sendFilterPolicies) {
        this.dataSource = dataSource;
        this.executor = executor;
        this.filterPolicy = bindFilterPolicy(sendFilterPolicies);
    }

    private SendFilterPolicy bindFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        SendFilterPolicy sendFilters = new NotConnected();
        for (SendFilterPolicy filter : sendFilterPolicies) {
            sendFilters = sendFilters.and(filter);
        }
        return sendFilters;
    }

    public void send(Socket self, Long roomId, SendMessage sendMessage) throws NoParticipantException {
        Set<Socket> sockets = dataSource.findRoomBy(roomId);
        List<Socket> targetSockets = sockets.stream().filter(socket -> filterPolicy.shouldSend(socket, self)).toList();
        if (targetSockets.isEmpty()) {
            throw new NoParticipantException("No participants in the room: " + roomId);
        }

        for (Socket socket : targetSockets) {
            executor.execute(() -> {
                try {
                    sendMessage.send(socket.getOutputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    public static class BroadcastExceptSelf implements SendFilterPolicy {
        @Override
        public boolean shouldSend(Socket receiver, Socket sender) {
            return !receiver.equals(sender);
        }
    }

    private static class NotConnected implements SendFilterPolicy {
        @Override
        public boolean shouldSend(Socket receiver, Socket sender) {
            return receiver.isConnected() && sender.isConnected();
        }
    }
}
