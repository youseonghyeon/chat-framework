package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
public class NioChannelListenEventLoop implements Runnable {

    private final Logger log = LoggerFactory.getLogger(NioChannelListenEventLoop.class);

    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private final MessageReader messageReader;
    private ExecutorService channelReadExecutor;

    public NioChannelListenEventLoop(Selector selector, ServerSocketChannel serverSocketChannel, MessageReader messageReader) {
        this.selector = selector;
        this.serverSocketChannel = serverSocketChannel;
        this.messageReader = messageReader;
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        this.channelReadExecutor = Executors.newFixedThreadPool(50);

        executorService.submit(this::runLoop);

        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
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
        Message message = messageReader.apply(clientChannel);

    }

    private void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

}
