package io.github.youseonghyeon.config;

import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

public class ChattingEngineConfig {

    private RoomSelector roomSelector;
    private ChattingDataSource chattingDataSource;
    private Integer coreThreadPoolSize;
    private Integer maxThreadPoolSize;
    private Callable<Socket> acceptPolicy;

    public ChattingEngineConfig roomSelector(RoomSelector roomSelector) {
        return this;
    }

    public ChattingEngineConfig datasource(ChattingDataSource chattingDataSource) {
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

    public ChattingEngineConfig acceptPolicy(Callable<Socket> acceptPolicy) {
        this.acceptPolicy = acceptPolicy;
        return this;

    }


    public RoomSelector getRoomSelector() {
        return roomSelector;
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

    public Callable<Socket> getAcceptPolicy() {
        return acceptPolicy;
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
}
