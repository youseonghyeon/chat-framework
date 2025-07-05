package io.github.youseonghyeon.config;

import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.config.serializer.MessageReader;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;

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
public class ChattingEngineConfig {

    private static final Logger log = LoggerFactory.getLogger(ChattingEngineConfig.class);
    private RoomSelector<?> roomSelector;
    private Integer coreThreadPoolSize;
    private Integer maxThreadPoolSize;
    private SendFilterPolicy sendFilterPolicy;
    private final Map<EventType, MessageSubscriber> messageSubscriberMap = new HashMap<>();
    private boolean useInvertedIndexSessionStore = false;
    private RejectedExecutionHandler rejectedExecutionHandler;
    private int port;

    /**
     * 룸 선택 전략을 설정합니다.
     *
     * @param roomSelector 소켓과 컨텍스트에 따라 룸을 선택하는 전략
     * @return 체이닝 가능한 현재 설정 인스턴스
     */
    public <T> ChattingEngineConfig roomSelector(RoomSelector<T> roomSelector) {
        this.roomSelector = roomSelector;
        return this;
    }

    /**
     * 여러 개의 송신 필터를 조합하여 하나의 정책으로 병합합니다.
     * 내부적으로 기본 필터 {@link NotConnected}가 항상 포함됩니다.
     *
     * @param sendFilterPolicies 조합할 필터들
     * @return 병합된 단일 필터
     */
    private SendFilterPolicy bindFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        SendFilterPolicy sendFilters = new NotConnected();
        for (SendFilterPolicy filter : sendFilterPolicies) {
            sendFilters = sendFilters.and(filter);
        }
        return sendFilters;
    }

    /**
     * 스레드 풀의 코어 및 최대 크기를 설정합니다.
     *
     * @param coreThreadPoolSize 코어 스레드 수
     * @param maxThreadPoolSize  최대 스레드 수
     * @return 체이닝 가능한 현재 설정 인스턴스
     */
    public ChattingEngineConfig threadPool(int coreThreadPoolSize, int maxThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        return this;
    }

    /**
     * 스레드 풀 설정에 거절 처리 핸들러를 추가로 설정합니다.
     *
     * @param coreThreadPoolSize       코어 스레드 수
     * @param maxThreadPoolSize        최대 스레드 수
     * @param rejectedExecutionHandler 거절 처리 핸들러
     * @return 체이닝 가능한 현재 설정 인스턴스
     * @deprecated 이 기능의 외부 제공 여부는 향후 검토 예정입니다.
     */
    @Deprecated
    public ChattingEngineConfig threadPool(int coreThreadPoolSize, int maxThreadPoolSize, RejectedExecutionHandler rejectedExecutionHandler) {
        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        return this;
    }

    /**
     * 송신 필터 정책을 설정합니다.
     *
     * @param sendFilterPolicies 적용할 필터들 (AND 방식으로 병합됨)
     * @return 체이닝 가능한 현재 설정 인스턴스
     */
    public ChattingEngineConfig sendFilterPolicy(SendFilterPolicy... sendFilterPolicies) {
        sendFilterPolicy = bindFilterPolicy(sendFilterPolicies);
        return this;
    }


    public ChattingEngineConfig addMessageSubscriber(EventType eventType, MessageSubscriber messageSubscriber) {
        messageSubscriberMap.put(eventType, messageSubscriber);
        return this;
    }

    /**
     * 역방향 인덱스 기반 세션 저장소 사용 여부를 설정합니다.
     *
     * @param useInvertedIndex true일 경우 역방향 인덱스를 활성화
     * @return 체이닝 가능한 현재 설정 인스턴스
     */
    public ChattingEngineConfig useInvertedIndexSessionStore(boolean useInvertedIndex) {
        this.useInvertedIndexSessionStore = useInvertedIndex;
        return this;
    }

    public ChattingEngineConfig port(int port) {
        this.port = port;
        return this;
    }

    // -- Getters

    @SuppressWarnings("unchecked")
    public <T> RoomSelector<T> getRoomSelector() {
        return (RoomSelector<T>) roomSelector;
    }

    public Integer getCoreThreadPoolSize() {
        return coreThreadPoolSize;
    }

    public Integer getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public SendFilterPolicy getSendFilterPolicy() {
        return sendFilterPolicy;
    }

    public Map<EventType, MessageSubscriber> getMessageSubscriberMap() {
        return messageSubscriberMap;
    }

    public boolean isUseInvertedIndexSessionStore() {
        return useInvertedIndexSessionStore;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    public int getPort() {
        return port;
    }

    public MessageReader getMessageReader() {
        // TODO 메시지 리더 구현 필요
        throw new UnsupportedOperationException("getMessageReader() is not implemented yet.");
    }

    /**
     * 송신자 본인을 제외하고 메시지를 브로드캐스트하는 기본 필터입니다.
     */
    public static class BroadcastExceptSelf implements SendFilterPolicy {
        @Override
        public boolean shouldSend(Socket receiver, Socket sender) {
            return !receiver.equals(sender);
        }
    }

    /**
     * 양쪽 소켓이 연결 상태일 때만 메시지를 송신하는 필터입니다.
     * 기본적으로 모든 필터 체인 앞단에 적용됩니다.
     */
    private static class NotConnected implements SendFilterPolicy {
        @Override
        public boolean shouldSend(Socket receiver, Socket sender) {
            return receiver.isConnected() && sender.isConnected();
        }
    }
}
