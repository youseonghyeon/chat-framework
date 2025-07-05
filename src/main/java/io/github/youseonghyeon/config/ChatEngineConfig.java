package io.github.youseonghyeon.config;

import io.github.youseonghyeon.config.adapter.MessageReceiver;
import io.github.youseonghyeon.config.adapter.MessageSender;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code ChattingEngine}의 구성 설정을 정의하는 클래스입니다.
 *
 * <p>이 클래스는 체이닝 기반의 설정 메서드를 제공하여, 사용자 정의 룸 분기, 스레드 풀 크기,
 * 메시지 송신 필터, 브로드캐스터 등을 손쉽게 구성할 수 있도록 돕습니다.</p>
 *
 * <p>설정이 지정되지 않은 항목은 {@code ChattingEngine} 내부에서 기본값으로 초기화됩니다.</p>
 *
 * <p>사용 예:
 * <pre>{@code
 * ChattingEngineConfig config = new ChattingEngineConfig()
 *     .roomSelector(new MyRoomSelector())
 *     .threadPool(10, 50)
 *     .sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf())
 *     .useInvertedIndexSessionStore(true);
 * }</pre>
 * </p>
 */
public class ChatEngineConfig {

    private static final Logger log = LoggerFactory.getLogger(ChatEngineConfig.class);

    private SendFilterPolicy sendFilterPolicy;
    private int port;
    private final Map<EventType, MessageSubscriber> messageSubscriberMap = new HashMap<>();
    private MessageSender messageSender;
    private MessageReceiver messageReceiver;

    /**
     * 여러 개의 송신 필터를 조합하여 하나의 정책으로 병합합니다.
     * 내부적으로 기본 필터 {@link NotConnected}가 항상 포함됩니다.
     *
     * @param sendFilterPolicies 조합할 필터들
     * @return 병합된 단일 필터
     */
    private SendFilterPolicy bindFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        SendFilterPolicy sendFilters = new SendFilterPolicy.NotConnected();
        for (SendFilterPolicy filter : sendFilterPolicies) {
            sendFilters = sendFilters.and(filter);
        }
        return sendFilters;
    }

    /**
     * 송신 필터 정책을 설정합니다.
     *
     * @param sendFilterPolicies 적용할 필터들 (AND 방식으로 병합됨)
     * @return 체이닝 가능한 현재 설정 인스턴스
     */
    public ChatEngineConfig sendFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        sendFilterPolicy = bindFilterPolicy(sendFilterPolicies);
        return this;
    }

    public ChatEngineConfig addMessageSubscriber(EventType eventType, MessageSubscriber messageSubscriber) {
        messageSubscriberMap.put(eventType, messageSubscriber);
        return this;
    }

    public ChatEngineConfig port(int port) {
        this.port = port;
        return this;
    }

    public ChatEngineConfig messageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
        return this;
    }

    public ChatEngineConfig messageReceiver(MessageReceiver messageReceiver) {
        this.messageReceiver = messageReceiver;
        return this;
    }

    // -- Getters

    public SendFilterPolicy getSendFilterPolicy() {
        return sendFilterPolicy;
    }

    public Map<EventType, MessageSubscriber> getMessageSubscriberMap() {
        return messageSubscriberMap;
    }

    public int getPort() {
        return port;
    }

    public MessageSender getMessageSender() {
        return messageSender;
    }

    public MessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    @Override
    public String toString() {
        return "sendFilterPolicy=" + sendFilterPolicy +
               "\n\tport=" + port +
               "\n\tmessageSubscriberMap=" + messageSubscriberMap.keySet() +
               "\n\tmessageSender=" + messageSender.getClass().getName() +
               "\n\tmessageReceiver=" + messageReceiver.getClass().getName();
    }
}
