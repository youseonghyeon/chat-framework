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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ChatEngine은 채팅 애플리케이션의 생명주기, 설정, 관리 기능을 담당하는 클래스입니다.
 * 이 클래스는 AbstractEngineLifecycle을 상속하며, 자원 초기화, 기본 설정 구성,
 * 엔진 구동을 위한 메서드들을 제공합니다.
 * <p>
 * 이 엔진은 채팅방 생성 및 관리, 채팅 관련 이벤트 처리,
 * 구독자에게 메시지를 발행하는 등의 기능을 지원합니다.
 */
public class ChatEngine extends AbstractEngineLifecycle {

    private final Logger log = LoggerFactory.getLogger(ChatEngine.class);

    private ChatEngineConfig config;
    private ChatEventPublisher chatEventPublisher;
    private ChannelListener channelListener;
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();

    public void setConfig(@NotNull Function<ChatEngineConfig, ChatEngineConfig> configChain) {
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

        Map<EventType, MessageSubscriber> messageSubscriberMap = config.getMessageSubscriberMap();
        messageSubscriberMap.computeIfAbsent(EventType.ENTER, type -> new EnterRoom(chatRoomMap, config.getMessageSender()));
        messageSubscriberMap.computeIfAbsent(EventType.LEAVE, type -> new LeaveRoom(chatRoomMap));
        messageSubscriberMap.computeIfAbsent(EventType.USER_SEND, type -> new SendMessage(chatRoomMap));
    }

    @Override
    protected void initResource() {
        this.chatEventPublisher = new ChatEventPublisher();
        this.channelListener = new ChannelListener(config.getPort(), config.getMessageReceiver(), chatEventPublisher);

        config.getMessageSubscriberMap().forEach((eventType, messageSubscriber) -> {
            messageSubscriber.init();
            chatEventPublisher.registerSubscriber(eventType, messageSubscriber);
        });
        log.info("ChatEngineConfig: {}", config);

    }

    @Override
    protected void startEngine() {
        channelListener.run();
    }


}
