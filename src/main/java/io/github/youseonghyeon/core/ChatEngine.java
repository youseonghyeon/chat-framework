package io.github.youseonghyeon.core;

import io.github.youseonghyeon.broadcast.no.NoOpBroadcaster;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.config.ChattingEngineConfig;
import io.github.youseonghyeon.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.core.event.ChatEventPublisher;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 엔진 세팅 및 연계 서비스 실행 담당
 */
public class ChatEngine extends AbstractEngineLifecycle {

    private final Logger log = LoggerFactory.getLogger(ChatEngine.class);

    private ChattingEngineConfig engineConfig;
    private ChatEventPublisher chatEventPublisher;
    private ChannelListener channelListener;

    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        Objects.requireNonNull(configChain, "Config chain must not be null");
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    @Override
    protected void initDefaultConfigIfAbsent() {
        if (engineConfig.getRoomSelector() == null) engineConfig.roomSelector(new PublicSquareRoomSelector<>());

        if (engineConfig.getSendFilterPolicy() == null)
            engineConfig.sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf());

        Map<EventType, MessageSubscriber> messageSubscriberMap = engineConfig.getMessageSubscriberMap();
        messageSubscriberMap.computeIfAbsent(EventType.BROADCAST, k -> new NoOpBroadcaster());
    }

    @Override
    protected void initResource() {
        SendFilterPolicy sendFilterPolicy = engineConfig.getSendFilterPolicy();
        this.channelListener = new ChannelListener(engineConfig.getPort(), engineConfig.getMessageReader(), chatEventPublisher);
        this.chatEventPublisher = new ChatEventPublisher();

        Map<EventType, MessageSubscriber> messageSubscriberMap = engineConfig.getMessageSubscriberMap();

        messageSubscriberMap.forEach((eventType, messageSubscriber) -> {
            messageSubscriber.init();
            chatEventPublisher.registerSubscriber(eventType, messageSubscriber);
        });
    }

    @Override
    protected void startEngine() {
        channelListener.run();
    }

}
