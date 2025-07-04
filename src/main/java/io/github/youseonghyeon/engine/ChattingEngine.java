package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;
import io.github.youseonghyeon.broadcast.impl.NoOpBroadcaster;
import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.engine.config.RoomSelector;
import io.github.youseonghyeon.engine.config.SendFilterPolicy;
import io.github.youseonghyeon.session.SessionStore;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 채팅 시스템의 핵심 엔진 클래스입니다.
 *
 * <p>이 클래스는 채팅 서버의 기본 구성 요소를 초기화하고 관리하며, 소켓을 특정 채팅방(Room)에
 * 참여시키거나 퇴장시키는 역할을 합니다. 내부적으로 스레드 풀을 구성하고, 채팅 메시지 송신을
 * 담당하는 {@link ChatManager}를 초기화합니다.</p>
 *
 * <p>기본적으로 RoomSelector가 설정되지 않은 경우 {@link PublicSquareRoomSelector}를 사용하여
 * 모든 클라이언트를 단일 공개 채팅방(광장)에 참여시키도록 동작합니다.</p>
 *
 * <p>사용자는 {@link ChattingEngineConfig}를 통해 설정을 전달할 수 있으며,
 * 참여 시 context 객체를 통해 다양한 기준으로 룸을 분기할 수 있습니다.</p>
 *
 * <p>예상되는 구성 요소:
 * <ul>
 *   <li>RoomSelector: 소켓 + context를 기반으로 룸을 결정</li>
 *   <li>InMemorySessionStore: 룸-소켓 관계를 메모리에 저장</li>
 *   <li>ChatManager: 메시지 브로드캐스트 처리</li>
 * </ul>
 * </p>
 * <p>
 * 사용 예:
 * <pre>{@code
 * ChattingEngine engine = new ChattingEngine();
 * engine.setConfig(config -> config.roomSelector(new CustomRoomSelector()));
 * engine.start();
 *
 * long roomId = engine.participate(socket, context);
 * engine.leave(socket, context);
 * }</pre>
 *
 * @author 유성현
 * @version preview-1.0.0
 */
@Deprecated(forRemoval = true)
public class ChattingEngine extends AbstractEngineLifecycle {

    private ChattingEngineConfig engineConfig;
    private SessionStore sessionStore;
    private ThreadPoolExecutor engineExecutor;
    private ChatManager chatManager;

    /**
     * 사용자로부터 전달된 설정 체인을 통해 {@link ChattingEngineConfig}를 구성합니다.
     * 이 메서드는 엔진을 시작하기 전에 반드시 호출되어야 합니다.
     *
     * @param configChain 설정을 구성하는 람다 함수 또는 메서드 참조
     *                    {@code config -> config.roomSelector(...).threadPoolSize(...)} 와 같이 사용됩니다.
     */
    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        Objects.requireNonNull(configChain, "Config chain must not be null");
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    /**
     * 내부적으로 초기화된 {@link ChatManager}를 반환합니다.
     * 메시지 송신 등과 관련된 작업은 이 객체를 통해 처리됩니다.
     *
     * @return 초기화된 ChatManager 인스턴스
     */
    public ChatManager chatManager() {
        Objects.requireNonNull(engineConfig, "ChattingEngineConfig is not initialized. Please call start() first.");
        return chatManager;
    }

    /**
     * {@link ThreadPoolExecutor}를 초기화합니다.
     * core/max/thread queue 크기는 설정값이 없을 경우 기본값으로 대체됩니다.
     * CallerRunsPolicy를 통해 백프레셔 정책을 우선 적용합니다.
     */
    @Override
    protected void initThreadPool() {
        final int coreSize = withDefault(engineConfig.getCoreThreadPoolSize(), 30);
        final int maxSize = withDefault(engineConfig.getMaxThreadPoolSize(), 100);
        final int queueSize = reasonableQueueSize(maxSize);
        // TODO 백프레셔 선 적용하고 추후 사용자에게 수정 기능을 제공해야 할지 검토 필요
        final RejectedExecutionHandler rejectedHandler = withDefault(engineConfig.getRejectedExecutionHandler(), new ThreadPoolExecutor.CallerRunsPolicy());
        engineExecutor = new ThreadPoolExecutor(coreSize, maxSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), rejectedHandler);
        engineExecutor.prestartAllCoreThreads();
        System.out.println("Chatting Engine Thread initialized with core size: " + coreSize + ", max size: " + maxSize + ", queue size: " + queueSize);
    }

    /**
     * 설정값이 지정되지 않은 경우, 기본값을 자동으로 설정합니다.
     * <p>
     * 예를 들어, RoomSelector, SendFilterPolicy, Broadcaster가 null일 경우,
     * 각각 기본 구현체를 주입합니다.
     * </p>
     * <ul>
     *     <li>{@link PublicSquareRoomSelector}: 기본 룸 선택자</li>
     *     <li>{@link ChattingEngineConfig.BroadcastExceptSelf}: 기본 송신 필터</li>
     *     <li>{@link NoOpBroadcaster}: 브로드캐스트 미처리 기본 동작</li>
     * </ul>
     */
    @Override
    protected void initDefaultConfigIfAbsent() {
        if (engineConfig.getRoomSelector() == null) engineConfig.roomSelector(new PublicSquareRoomSelector<>());
        if (engineConfig.getSendFilterPolicy() == null)
            engineConfig.sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf());
        if (engineConfig.getBroadcaster() == null) engineConfig.messageBroadcaster(new NoOpBroadcaster());
        sessionStore = engineConfig.isUseInvertedIndexSessionStore()
                ? new SessionStore().enableReverseLookup()
                : new SessionStore();
    }

    /**
     * 초기화된 {@link ChatManager} 인스턴스를 반환합니다.
     * <p>
     * 채팅 메시지 송신 또는 필터링 등과 같은 고수준 채팅 제어 기능을 제공하는 컴포넌트입니다.
     * </p>
     */
    @Override
    protected void initResource() {
        SendFilterPolicy sendFilterPolicy = engineConfig.getSendFilterPolicy();
        MessageBroadcaster broadcaster = engineConfig.getBroadcaster();
        this.chatManager = new ChatManager(sessionStore, engineExecutor, sendFilterPolicy, broadcaster);
    }

    /**
     * 엔진의 스레드 풀을 안전하게 종료합니다.
     * 일정 시간 내 종료되지 않으면 강제 종료를 수행합니다.
     */
    @Override
    protected void stopThreadPool() {
        engineExecutor.shutdown();
        try {
            boolean b = engineExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!b) {
                Thread.sleep(10_000); // wait 10 seconds
                engineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void destroyAllSessions() {
        sessionStore.getAllSessions().forEach(socket -> {
            try {
                if (socket == null || !socket.isConnected()) return;
                socket.close();
            } catch (IOException e) {
                // TODO 세션 종료 실패에 대한 정책 수립 필요
                e.printStackTrace();
            }
        });
    }

    /**
     * 특정 소켓과 context 정보를 바탕으로 룸에 참여시킵니다.
     *
     * @param socket  참여할 사용자의 소켓
     * @param context 룸 선택에 사용될 컨텍스트 (예: 사용자 정보, 위치 등)
     * @param <T>     context의 타입
     * @return 참여한 룸의 ID
     */
    public <T> long participate(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        sessionStore.join(socket, roomId);
        return roomId;
    }

    /**
     * 특정 소켓과 context 정보를 바탕으로 룸에서 퇴장 처리합니다.
     *
     * @param socket  퇴장할 사용자의 소켓
     * @param context 룸 선택에 사용될 컨텍스트 (예: 사용자 정보, 위치 등)
     * @param <T>     context의 타입
     */
    public <T> void leave(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        sessionStore.leave(socket, roomId);
    }
}
