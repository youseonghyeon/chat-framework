package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.adapter.MessageReceiver;
import io.github.youseonghyeon.core.dto.Message;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.core.exception.ChannelReadException;
import io.github.youseonghyeon.core.exception.InitChatServiceException;
import io.github.youseonghyeon.utils.ExecutorCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * {@code ChannelListener} is the core event loop responsible for handling client socket
 * connections in a non-blocking I/O (NIO) manner using Java's {@link Selector} API.
 *
 * <p>It accepts new client connections, reads inbound messages from {@link SocketChannel}s via
 * a {@link MessageReceiver}, and dispatches them to an internal event system using
 * {@link ChatEventPublisher}.</p>
 *
 * <p>This class runs a selector event loop in a dedicated thread and delegates channel reads
 * to a thread pool. It also registers a JVM shutdown hook for graceful resource cleanup.</p>
 *
 * <p><strong>Main responsibilities:</strong>
 * <ul>
 *     <li>Initialize and bind a non-blocking server socket channel</li>
 *     <li>Process I/O events using a selector</li>
 *     <li>Handle reads concurrently via a thread pool</li>
 *     <li>Gracefully shut down all executors on JVM exit</li>
 * </ul>
 * </p>
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

    private volatile boolean shutdown = false;

    /**
     * Constructs a new {@code ChannelListener} that binds to the specified port and prepares
     * a selector to handle I/O events.
     *
     * @param port               the TCP port to bind the server socket
     * @param messageReceiver    the component used to parse incoming data from clients
     * @param chatEventPublisher the event dispatcher for delivering parsed messages
     * @throws InitChatServiceException if the selector or server socket channel fails to initialize
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
     * Opens and registers a non-blocking {@link ServerSocketChannel} to the selector
     * for accepting incoming connections.
     *
     * @param selector the selector to register with
     * @param port     the port to bind the server socket
     * @return a configured {@code ServerSocketChannel} registered with the selector
     * @throws IOException if binding or channel setup fails
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
     * Starts the selector loop and a thread pool for channel read operations.
     * Also registers a JVM shutdown hook to cleanly terminate both executors.
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
     * Runs the main selector loop, blocking on {@code select()} and processing I/O events
     * when available.
     *
     * <p>Selector exceptions are propagated as unchecked exceptions.</p>
     */
    private void runLoop() {
        while (!shutdown) {
            try {
                selector.select();
                processSelectedKeys();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Processes all ready {@link SelectionKey}s from the selector.
     * Accepts new connections or delegates read-ready channels to worker threads.
     */
    private void processSelectedKeys() {
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
                channelReadExecutor.submit(() -> handleRead(key, client));
            }
        }
    }

    /**
     * Handles a new client connection and registers it for read events.
     *
     * @param serverSocketChannel the server channel accepting new clients
     * @param selector            the selector to register the client with
     */
    private void handleAccept(ServerSocketChannel serverSocketChannel, Selector selector) {
        try {
            SocketChannel client = serverSocketChannel.accept();
            client.configureBlocking(false);
            selector.wakeup();
            client.register(selector, SelectionKey.OP_READ);
        } catch (ClosedChannelException e) {
            log.error("Failed to accept client connection: Channel is closed", e);
        } catch (IOException e) {
            log.error("Failed to accept client connection", e);
        }
    }

    /**
     * Reads a message from the client socket and publishes it to the internal event bus.
     * Restores the {@link SelectionKey}'s interest in read events after processing.
     *
     * @param key     the selection key associated with the socket
     * @param channel the client socket channel
     */
    private void handleRead(SelectionKey key, SocketChannel channel) {
        try {
            Message message = messageReceiver.read(channel);
            chatEventPublisher.publish(message);
        } catch (ChannelReadException e) {
            log.error("Failed to read from channel: {}", channel, e);
        } finally {
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            key.selector().wakeup();
        }
    }

}
