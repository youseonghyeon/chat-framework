package io.github.youseonghyeon.core;

import io.github.youseonghyeon.config.ChatEngineConfig;
import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageReceiver;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageSender;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import io.github.youseonghyeon.core.event.action.EnterRoom;
import io.github.youseonghyeon.core.event.action.LeaveRoom;
import io.github.youseonghyeon.core.event.action.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 엔진 세팅 및 연계 서비스 실행 담당
 */
public class ChatEngine extends AbstractEngineLifecycle {

    private final Logger log = LoggerFactory.getLogger(ChatEngine.class);

    private ChatEngineConfig config;
    private ChatEventPublisher chatEventPublisher;
    private ChannelListener channelListener;
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();

    public void setConfig(Function<ChatEngineConfig, ChatEngineConfig> configChain) {
        Objects.requireNonNull(configChain, "Config chain must not be null");
        this.config = configChain.apply(new ChatEngineConfig());
    }

    @Override
    protected void initDefaultConfigIfAbsent() {
        if (config.getSendFilterPolicy() == null)
            config.sendFilterPolicy(new SendFilterPolicy.BroadcastExceptSelf());

        if (config.getMessageReceiver() == null && config.getMessageSender() == null) {
            config.messageReceiver(new DefaultMessageReceiver());
            config.messageSender(new DefaultMessageSender());
        }
    }

    @Override
    protected void initResource() {
        SendFilterPolicy sendFilterPolicy = config.getSendFilterPolicy();
        this.chatEventPublisher = new ChatEventPublisher();
        this.channelListener = new ChannelListener(config.getPort(), config.getMessageReceiver(), chatEventPublisher);

        Map<EventType, MessageSubscriber> messageSubscriberMap = config.getMessageSubscriberMap();

        // for test
        messageSubscriberMap.put(EventType.ENTER, new EnterRoom(chatRoomMap));
        messageSubscriberMap.put(EventType.LEAVE, new LeaveRoom(chatRoomMap));
        messageSubscriberMap.put(EventType.USER_SEND, new SendMessage(chatRoomMap));
        //

        messageSubscriberMap.forEach((eventType, messageSubscriber) -> {
            messageSubscriber.init();
            chatEventPublisher.registerSubscriber(eventType, messageSubscriber);
        });

    }

    @Override
    protected void startEngine() {
        log.info("ChatEngineConfig: {}", config);
        channelListener.run();
    }


}
