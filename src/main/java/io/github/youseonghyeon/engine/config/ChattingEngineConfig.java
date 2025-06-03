package io.github.youseonghyeon.engine.config;

import java.net.Socket;

public class ChattingEngineConfig {

    private RoomSelector<?> roomSelector;
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

    public ChattingEngineConfig threadPoolSize(int coreThreadPoolSize, int maxThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> RoomSelector<T> getRoomSelector() {
        return (RoomSelector<T>) roomSelector;
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
