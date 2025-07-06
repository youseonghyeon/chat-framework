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
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 사용자가 입장/퇴장 하고 socketChannel 에 메시지를 받는 것들을 처리함
 */
public class ChannelListener implements Runnable {

    private final Logger log = LoggerFactory.getLogger(ChannelListener.class);

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final MessageReceiver messageReceiver;
    private final ChatEventPublisher chatEventPublisher;
    private ExecutorService channelReadExecutor;
    private ExecutorService eventLoopExecutor;

    private ReadFailureHandler readFailureHandler;

    private volatile boolean shutdown = false;

    public ChannelListener(int port, MessageReceiver messageReceiver, ChatEventPublisher chatEventPublisher) {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = openServer(selector, port);
        } catch (IOException e) {
            throw new InitChatServiceException(e);
        }
        this.messageReceiver = messageReceiver;
        this.chatEventPublisher = chatEventPublisher;
    }

    private static ServerSocketChannel openServer(Selector selector, int p) throws IOException {
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

        // TODO Thread Pool Executor 로 동적 크기 조정 적용 및 Backpressure 처리 필요
        this.channelReadExecutor = Executors.newFixedThreadPool(50);

        eventLoopExecutor.submit(this::runLoop);

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    shutdown = true;
                    ExecutorCoordinator.shutdownSequential(eventLoopExecutor, channelReadExecutor);
                }, "EventLoopShutdownHook"));
    }

    public ByteBuffer buffer = ByteBuffer.allocate(1024);

    private void runLoop() {
        while (!shutdown) {
            try {
                selector.select();
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
            } catch (IOException e) {
                log.error("Failed to select channels", e);
                // TODO 루프 내 리소스 초기화 기능 추가
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

}
