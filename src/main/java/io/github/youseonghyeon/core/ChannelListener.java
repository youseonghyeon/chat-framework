package io.github.youseonghyeon.core;

import io.github.youseonghyeon.utils.ExecutorCoordinator;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.config.serializer.MessageReader;
import io.github.youseonghyeon.exception.InitChatServiceException;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    private final MessageReader messageReader;
    private final ChatEventPublisher chatEventPublisher;
    private ExecutorService channelReadExecutor;
    private ExecutorService eventLoopExecutor;


    public ChannelListener(int port, MessageReader messageReader, ChatEventPublisher chatEventPublisher) {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress local = new InetSocketAddress(port);
            this.serverSocketChannel.bind(local);
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new InitChatServiceException(e);
        }
        this.messageReader = messageReader;
        this.chatEventPublisher = chatEventPublisher;
    }

    @Override
    public void run() {
        this.eventLoopExecutor = Executors.newSingleThreadExecutor();
        this.channelReadExecutor = Executors.newFixedThreadPool(50); // config 대상

        eventLoopExecutor.submit(this::runLoop);

        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> ExecutorCoordinator.shutdownSequential(eventLoopExecutor, channelReadExecutor), "EventLoopShutdownHook"));
    }

    public void runLoop() {
        while (true) {
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
                        SocketChannel client = (SocketChannel) key.channel();
                        channelReadExecutor.submit(() -> handleRead(client));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleRead(SocketChannel clientChannel) {
        log.trace("Handling read from channel: {}", clientChannel);
        Message message = messageReader.read(clientChannel);
        String roomId = message.roomId();
        if (roomId == null || roomId.isEmpty()) {
            log.warn("Received message without room ID: {}", message);
            return; // 메시지에 roomId 가 없으면 무시
        }
        chatEventPublisher.publish(message);
    }

    private void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

}
