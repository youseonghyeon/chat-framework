package io.github.youseonghyeon;

import io.github.youseonghyeon.config.ChattingEngineConfig;
import io.github.youseonghyeon.config.RoomSelector;
import io.github.youseonghyeon.config.datasource.ChattingDataSource;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Function;

public class ChattingEngine extends AbstractEngineLifecycle {

    private final int port;
    private ChattingEngineConfig engineConfig;
    private ChattingDataSource dataSource;
    private ThreadPoolExecutor executor;
    private ChatManger chatManger;
    private ServerSocket serverSocket;

    public ChattingEngine(int port) {
        this.port = port;
    }

    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    public int getPort() {
        return port;
    }

    public ChatManger chatManger() {
        return chatManger;
    }

    @Override
    protected void initServerSocket() {
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535");
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server socket initialized on port: " + port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (engineConfig.getAcceptPolicy() == null) {
            createDefaultAcceptThread(serverSocket);
        }
    }

    private void createDefaultAcceptThread(ServerSocket serverSocket) {
        int acceptThreadSize = 1;
        ExecutorService es = Executors.newFixedThreadPool(acceptThreadSize);
        for (int i = 0; i < acceptThreadSize; i++) {
            es.execute(() -> {
                try {
                    Socket socket = serverSocket.accept();
                    RoomSelector roomSelector = engineConfig.getRoomSelector();
                    if (roomSelector == null) {
                        throw new IllegalStateException("RoomSelector must be set in the configuration");
                    }
                    participate(socket, roomSelector);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    protected void initThreadPool() {
        int coreSize = withDefault(engineConfig.getCoreThreadPoolSize(), 30);
        int maxSize = withDefault(engineConfig.getMaxThreadPoolSize(), 100);
        int queueSize = reasonableQueueSize(maxSize);
        executor = new ThreadPoolExecutor(coreSize, maxSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), new ThreadPoolExecutor.CallerRunsPolicy());
        if (coreSize >= 30) executor.prestartAllCoreThreads();
        System.out.println("ThreadPoolExecutor initialized with core size: " + coreSize + ", max size: " + maxSize + ", queue size: " + queueSize);
    }

    private <T> T withDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    protected void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initResource() {
        this.dataSource = engineConfig.getChattingDataSource();
        this.chatManger = new ChatManger(dataSource, executor);
    }

    @Override
    public void stopThreadPool() {
        executor.shutdown();
        try {
            boolean b = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!b) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void participate(Socket socket, RoomSelector roomSelector) {
        Long roomIdentifier = roomSelector.selectRoom();
        dataSource.registerSocketToRoom(socket, roomIdentifier);
    }

    public void leave(Socket socket, RoomSelector roomSelector) {
        Long roomIdentifier = roomSelector.selectRoom();
        dataSource.removeSocketFromRoom(socket, roomIdentifier);
    }
}
