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
 * {@code ChannelListener} is the core event loop responsible for handling client socket
 * connections in a non-blocking I/O (NIO) fashion using Java's {@link Selector} API.
 *
 * <p>It manages new client connections, reads inbound messages via a {@link MessageReceiver},
 * and dispatches them to the appropriate handlers using {@link ChatEventPublisher}.</p>
 *
 * <p>This class launches an event loop thread and a thread pool for concurrent channel reads,
 * and gracefully shuts down all resources via a JVM shutdown hook.</p>
 *
 * @see Selector
 * @see SocketChannel
 * @see MessageReceiver
 * @see ChatEventPublisher
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

    /**
     * Constructs a {@code ChannelListener} with a specific port, message reader, and event dispatcher.
     *
     * @param port the port number to bind the server socket
     * @param messageReceiver the component responsible for reading raw socket messages
     * @param chatEventPublisher the dispatcher that routes parsed messages to subscribers
     * @throws InitChatServiceException if the server socket fails to initialize
     */
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

    /**
     * Opens and registers a non-blocking {@link ServerSocketChannel} with the given {@link Selector}.
     *
     * @param selector the selector used for managing I/O events
     * @param port the port to bind the server socket
     * @return a configured {@code ServerSocketChannel}
     * @throws IOException if the socket cannot be opened or bound
     */
    private static ServerSocketChannel openPort(Selector selector, int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress local = new InetSocketAddress(port);
        ssc.bind(local);
        ssc.configureBlocking(false);
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        return ssc;
    }

    /**
     * Launches the event loop and channel read executor threads.
     * Registers a shutdown hook to cleanly terminate all threads.
     */
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

    /**
     * Main event loop that waits for selector events and delegates to appropriate handlers.
     * Also performs threshold-based error tracking for selector failures.
     */
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

    /**
     * Processes all ready {@link SelectionKey} instances registered to the selector.
     * Accepts new client connections or reads from active client channels.
     *
     * @throws IOException if channel read/write fails
     */
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

    /**
     * Handles exceptions during selector select operations and throttles excessive failures.
     *
     * @param e the caught I/O exception
     * @param errorDeque a deque tracking recent error timestamps
     */
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

    /**
     * Reads a message from a client and publishes it to the internal event bus.
     * Delegates exception handling to a custom {@link ReadFailureHandler} if configured.
     *
     * @param clientChannel the source client channel
     */
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

    /**
     * Accepts a new incoming client connection and registers it with the selector for read events.
     *
     * @param serverSocketChannel the bound server socket
     * @param selector the selector to register the client with
     * @throws IOException if client channel configuration or registration fails
     */
    private void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    /**
     * Checks whether error threshold has been exceeded for publishing engine recovery events.
     *
     * @param errorStartTime timestamp of the first error
     * @param currentMillis current time
     * @param threshold duration threshold in milliseconds
     * @return {@code true} if the error duration is within the given threshold
     */
    private boolean isThresholdExceeded(Long errorStartTime, Long currentMillis, Long threshold) {
        return errorStartTime >= currentMillis - threshold;
    }

}
