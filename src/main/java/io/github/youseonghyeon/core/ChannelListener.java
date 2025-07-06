package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.adapter.MessageReceiver;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.exception.ChannelReadException;
import io.github.youseonghyeon.exception.InitChatServiceException;
import io.github.youseonghyeon.exception.ReadFailureHandler;
import io.github.youseonghyeon.utils.ExecutorCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 사용자가 입장/퇴장 하고 socketChannel 에 메시지를 받는 것들을 처리함
 */
public class ChannelListener implements Runnable {

    private final Logger log = LoggerFactory.getLogger(ChannelListener.class);

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final MessageReceiver messageReceiver;
    private final ChatEventPublisher chatEventPublisher;
    private ThreadPoolExecutor channelReadExecutor;
    private ExecutorService eventLoopExecutor;

    private ReadFailureHandler readFailureHandler;

    private volatile boolean shutdown = false;

    public ChannelListener(int port, MessageReceiver messageReceiver, ChatEventPublisher chatEventPublisher) {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = openPort(selector, port);
        } catch (IOException e) {
            throw new InitChatServiceException(e);
        }
        this.messageReceiver = messageReceiver;
        this.chatEventPublisher = chatEventPublisher;
    }

    private static ServerSocketChannel openPort(Selector selector, int p) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress local = new InetSocketAddress(p);
        ssc.bind(local);
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        return ssc;
    }

    @Override
    public void run() {
        this.eventLoopExecutor = Executors.newSingleThreadExecutor();
        // TODO 우선 백프레셔를 기본값으로 설정하며, 추후 engine config 에서 불러올 수 있도록 변경 필요
        this.channelReadExecutor = new ThreadPoolExecutor(10, 50, 5, TimeUnit.MINUTES, new LinkedBlockingQueue<>(1000), new ThreadPoolExecutor.CallerRunsPolicy());
        this.channelReadExecutor.prestartAllCoreThreads();

        eventLoopExecutor.submit(this::runLoop);

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    shutdown = true;
                    ExecutorCoordinator.shutdownSequential(eventLoopExecutor, channelReadExecutor);
                }, "EventLoopShutdownHook"));
    }

    private void runLoop() {
        Deque<Long> errorDeque = new ArrayDeque<>();
        while (!shutdown) {
            try {
                selector.select();
                processSelectedKeys();
            } catch (IOException e) {
                handleSelectError(e, errorDeque);
            }
        }
    }

    private void processSelectedKeys() throws IOException {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> iter = selectedKeys.iterator();

        while (iter.hasNext()) {
            SelectionKey key = iter.next();
            iter.remove();
            if (key.isAcceptable()) {
                handleAccept(serverSocketChannel, selector);
            }
            if (key.isReadable()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                SocketChannel client = (SocketChannel) key.channel();
                channelReadExecutor.submit(() -> {
                    try {
                        handleRead(client);
                    } finally {
                        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                        selector.wakeup();
                    }
                });
            }
        }
    }

    private void handleSelectError(IOException e, Deque<Long> errorDeque) {
        log.error("Failed to select channels", e);
        errorDeque.add(System.currentTimeMillis());
        if (errorDeque.size() > 10) {
            if (isThresholdExceeded(errorDeque.getFirst(), System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(1))) {
                // TODO publish Engine Recovery Event
            } else {
                errorDeque.clear();
            }
        }
    }

    private void handleRead(SocketChannel clientChannel) {
        try {
            Message message = messageReceiver.read(clientChannel);
            chatEventPublisher.publish(message);
        } catch (ChannelReadException e) {
            log.error("Failed to read message from channel: {}", clientChannel, e);
            if (readFailureHandler != null) {
                readFailureHandler.handle(clientChannel, e);
            }
        }
    }

    private void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private boolean isThresholdExceeded(Long errorStartTime, Long currentMillis, Long threshold) {
        return errorStartTime >= currentMillis - threshold;
    }

}
