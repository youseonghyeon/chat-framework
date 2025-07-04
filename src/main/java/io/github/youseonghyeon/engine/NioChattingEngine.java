package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;
import io.github.youseonghyeon.broadcast.impl.NoOpBroadcaster;
import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.engine.config.SendFilterPolicy;
import io.github.youseonghyeon.engine.exception.InitServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 엔진 세팅 및 연계 서비스 실행 담당
 */
public class NioChattingEngine extends AbstractEngineLifecycle {

    private final Logger log = LoggerFactory.getLogger(NioChattingEngine.class);

    private ChattingEngineConfig engineConfig;
    private ThreadPoolExecutor engineExecutor;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;

    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        Objects.requireNonNull(configChain, "Config chain must not be null");
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    @Override
    protected void initThreadPool() {
        final int coreSize = withDefault(engineConfig.getCoreThreadPoolSize(), 30);
        final int maxSize = withDefault(engineConfig.getMaxThreadPoolSize(), 100);
        final int queueSize = reasonableQueueSize(maxSize);
        // TODO 백프레셔 선 적용하고 추후 사용자에게 수정 기능을 제공해야 할지 검토 필요
        final RejectedExecutionHandler rejectedHandler = withDefault(engineConfig.getRejectedExecutionHandler(), new ThreadPoolExecutor.CallerRunsPolicy());
        engineExecutor = new ThreadPoolExecutor(coreSize, maxSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), rejectedHandler);
        engineExecutor.prestartAllCoreThreads();
        System.out.println("Chatting Engine Thread initialized with core size: " + coreSize + ", max size: " + maxSize + ", queue size: " + queueSize);
    }

    @Override
    protected void initDefaultConfigIfAbsent() {
        if (engineConfig.getRoomSelector() == null) engineConfig.roomSelector(new PublicSquareRoomSelector<>());
        if (engineConfig.getSendFilterPolicy() == null)
            engineConfig.sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf());
        if (engineConfig.getBroadcaster() == null) engineConfig.messageBroadcaster(new NoOpBroadcaster());
    }

    @Override
    protected void initResource() {
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress local = new InetSocketAddress(engineConfig.getPort());
            serverSocketChannel.bind(local);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new InitServerException(e);
        }
        SendFilterPolicy sendFilterPolicy = engineConfig.getSendFilterPolicy();
        MessageBroadcaster broadcaster = engineConfig.getBroadcaster();
    }

    @Override
    protected void stopThreadPool() {
        engineExecutor.shutdown();
        try {
            boolean b = engineExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!b) {
                Thread.sleep(10_000); // wait 10 seconds
                engineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void destroyAllSessions() {

    }

    @Override
    protected void startEngine() {
        new NioChannelListenEventLoop(selector, serverSocketChannel, engineConfig.getMessageReader()).run();
    }


    // TODO event publisher 를 여기서 초기화 하고 실행함
}
