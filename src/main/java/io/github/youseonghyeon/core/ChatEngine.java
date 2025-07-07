package io.github.youseonghyeon.core;

import io.github.youseonghyeon.broadcast.no.NoOpsBroadcaster;
import io.github.youseonghyeon.config.ChatEngineConfig;
import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageReceiver;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageSender;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import io.github.youseonghyeon.core.event.command.EnterRoom;
import io.github.youseonghyeon.core.event.command.LeaveRoom;
import io.github.youseonghyeon.core.event.command.SendMessage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ChatEngine manages the lifecycle and configuration of a chat application engine.
 * It is responsible for initializing core resources, registering event subscribers,
 * and starting the message processing loop.
 *
 * <p>Typical usage:
 * <pre>{@code
 * ChatEngine engine = new ChatEngine();
 * engine.applyConfiguration(cfg -> cfg.port(9000));
 * engine.run(); // inherited from AbstractEngineLifecycle
 * }</pre>
 *
 * @see ChatEngineConfig
 * @see ChatRoom
 * @see ChatEventPublisher
 */
public class ChatEngine extends AbstractEngineLifecycle {

    private final Logger log = LoggerFactory.getLogger(ChatEngine.class);

    private ChatEngineConfig config;
    private ChatEventPublisher chatEventPublisher;
    private ChannelListener channelListener;
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();

    /**
     * Applies user-defined configuration using the given functional chain.
     * This should be called before {@link #launch()} to ensure the engine has all required settings.
     *
     * @param configChain a function to configure and return the final {@link ChatEngineConfig}
     * @throws NullPointerException if the provided configChain is null
     */
    public void applyConfiguration(@NotNull Function<ChatEngineConfig, ChatEngineConfig> configChain) {
        Objects.requireNonNull(configChain, "Config chain must not be null");
        this.config = configChain.apply(new ChatEngineConfig());
    }

    /**
     * Initializes default configuration values if user-defined values are not provided.
     * This method ensures all required components such as message sender/receiver and policies
     * are set before engine execution begins.
     */
    @Override
    protected void initializeDefaultConfiguration() {
        if (config.getSendFilterPolicy() == null)
            config.sendFilterPolicy(new SendFilterPolicy.BroadcastExceptSelf());

        if (config.getMessageReceiver() == null && config.getMessageSender() == null) {
            config.messageReceiver(new DefaultMessageReceiver());
            config.messageSender(new DefaultMessageSender());
        }

        Map<EventType, MessageSubscriber> messageSubscriberMap = config.getMessageSubscriberMap();
        messageSubscriberMap.computeIfAbsent(EventType.ENTER, type -> new EnterRoom(chatRoomMap, config.getMessageSender()));
        messageSubscriberMap.computeIfAbsent(EventType.LEAVE, type -> new LeaveRoom(chatRoomMap));
        messageSubscriberMap.computeIfAbsent(EventType.USER_SEND, type -> new SendMessage(chatRoomMap));

        if (config.getMessageBroadCaster() == null) {
            config.messageBroadCaster(new NoOpsBroadcaster());
        }
    }

    /**
     * Initializes core engine components such as event publisher and channel listener.
     * Registers all message subscribers declared in the configuration.
     *
     * <p>Must be called after {@link #initializeDefaultConfiguration()}.
     */
    @Override
    protected void initializeEngineComponents() {
        this.chatEventPublisher = new ChatEventPublisher();
        this.channelListener = new ChannelListener(config.getPort(), config.getMessageReceiver(), chatEventPublisher);

        // TODO 이쯤에서 kafka initialize 를 처리해야 함
        config.getMessageSubscriberMap()
                .forEach((eventType, messageSubscriber) -> chatEventPublisher.registerSubscriber(eventType, messageSubscriber));
        log.info("ChatEngineConfig: {}", config);

    }

    /**
     * Starts the chat engine by running the main channel listener.
     * This begins accepting socket connections and dispatching messages.
     */
    @Override
    protected void launch() {
        channelListener.run();
    }

    public ChatEventPublisher getEventPublisher() {
        return chatEventPublisher;
    }


}
