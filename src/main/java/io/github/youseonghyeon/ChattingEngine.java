package io.github.youseonghyeon;

import io.github.youseonghyeon.config.ChattingEngineConfig;
import io.github.youseonghyeon.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.config.RoomSelector;
import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ChattingEngine extends AbstractEngineLifecycle {

    private ChattingEngineConfig engineConfig;
    private ChattingDataSource dataSource;
    private ThreadPoolExecutor engineExecutor;
    private ChatManger chatManger;

    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    public ChatManger chatManger() {
        return chatManger;
    }

    @Override
    protected void initThreadPool() {
        int coreSize = withDefault(engineConfig.getCoreThreadPoolSize(), 30);
        int maxSize = withDefault(engineConfig.getMaxThreadPoolSize(), 100);
        int queueSize = reasonableQueueSize(maxSize);
        // TODO 백프레셔를 우선 적용하고 추후 CustomExecuteExceptionHandler 적용 가능하도록 수정 필요
        engineExecutor = new ThreadPoolExecutor(coreSize, maxSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), new ThreadPoolExecutor.CallerRunsPolicy());
        engineExecutor.prestartAllCoreThreads();
        System.out.println("ThreadPoolExecutor initialized with core size: " + coreSize + ", max size: " + maxSize + ", queue size: " + queueSize);
    }

    @Override
    protected void initRoomSelectorIfAbsent() {
        if (engineConfig.getRoomSelector() == null) {
            engineConfig.roomSelector(new PublicSquareRoomSelector());
        }
    }

    private <T> T withDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    protected void initResource() {
        this.dataSource = engineConfig.getChattingDataSource();
        this.chatManger = new ChatManger(dataSource, engineExecutor, engineConfig);
    }

    @Override
    public void stopThreadPool() {
        engineExecutor.shutdown();
        try {
            boolean b = engineExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!b) {
                engineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> long participate(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        dataSource.registerSocketToRoom(socket, roomId);
        return roomId;
    }

    public <T> void leave(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        dataSource.removeSocketFromRoom(socket, roomId);
    }
}
