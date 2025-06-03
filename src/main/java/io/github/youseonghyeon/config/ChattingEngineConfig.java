package io.github.youseonghyeon.config;

import io.github.youseonghyeon.SendFilterPolicy;
import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

public class ChattingEngineConfig {

    private RoomSelector<?> roomSelector;
    private ChattingDataSource chattingDataSource;
    private Integer coreThreadPoolSize;
    private Integer maxThreadPoolSize;
    private SendFilterPolicy sendFilterPolicy;

    public <T> ChattingEngineConfig roomSelector(RoomSelector<T> roomSelector) {
        this.roomSelector = roomSelector;
        return this;
    }

    public ChattingEngineConfig sendFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        sendFilterPolicy = bindFilterPolicy(sendFilterPolicies);
        return this;
    }

    private SendFilterPolicy bindFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        SendFilterPolicy sendFilters = new NotConnected();
        for (SendFilterPolicy filter : sendFilterPolicies) {
            sendFilters = sendFilters.and(filter);
        }
        return sendFilters;
    }

    public ChattingEngineConfig datasource(ChattingDataSource chattingDataSource) {
        this.chattingDataSource = chattingDataSource;
        return this;
    }

    public ChattingEngineConfig threadPoolExecutor(ThreadPoolExecutor executor) {
        return this;
    }

    public ChattingEngineConfig threadPoolSize(int coreThreadPoolSize, int maxThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> RoomSelector<T> getRoomSelector() {
        return (RoomSelector<T>) roomSelector;
    }

    public ChattingDataSource getChattingDataSource() {
        return chattingDataSource;
    }

    public Integer getCoreThreadPoolSize() {
        return coreThreadPoolSize;
    }

    public Integer getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public SendFilterPolicy getSendFilterPolicy() {
        return sendFilterPolicy;
    }

    @Override
    public String toString() {
        return "ChattingEngineConfig{" +
               "roomSelector=" + roomSelector +
               ", chattingDataSource=" + chattingDataSource +
               ", coreThreadPoolSize=" + coreThreadPoolSize +
               ", maxThreadPoolSize=" + maxThreadPoolSize +
               '}';
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
