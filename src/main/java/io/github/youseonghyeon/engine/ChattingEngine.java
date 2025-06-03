package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.engine.config.RoomSelector;
import io.github.youseonghyeon.session.InMemorySessionStore;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
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
 *
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
 */
public class ChattingEngine extends AbstractEngineLifecycle {

    private ChattingEngineConfig engineConfig;
    private InMemorySessionStore dataSource;
    private ThreadPoolExecutor engineExecutor;
    private ChatManager chatManager;

    public void setConfig(Function<ChattingEngineConfig, ChattingEngineConfig> configChain) {
        this.engineConfig = configChain.apply(new ChattingEngineConfig());
    }

    public ChatManager chatManager() {
        return chatManager;
    }

    @Override
    protected void initThreadPool() {
        int coreSize = withDefault(engineConfig.getCoreThreadPoolSize(), 30);
        int maxSize = withDefault(engineConfig.getMaxThreadPoolSize(), 100);
        int queueSize = reasonableQueueSize(maxSize);
        // TODO 백프레셔를 우선 적용하고 추후 CustomExecuteExceptionHandler 적용 가능하도록 수정 필요
        engineExecutor = new ThreadPoolExecutor(coreSize, maxSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize), new ThreadPoolExecutor.CallerRunsPolicy());
        engineExecutor.prestartAllCoreThreads();
        System.out.println("ThreadPoolExecutor initialized with core size: " + coreSize + ", max size: " + maxSize + ", queue size: " + queueSize);
    }

    @Override
    protected void initRoomSelectorIfAbsent() {
        if (engineConfig.getRoomSelector() == null) {
            engineConfig.roomSelector(new PublicSquareRoomSelector<>());
        }
    }

    private <T> T withDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    @Override
    protected void initResource() {
        this.dataSource = new InMemorySessionStore();
        this.chatManager = new ChatManager(dataSource, engineExecutor, engineConfig);
    }

    @Override
    protected void stopThreadPool() {
        engineExecutor.shutdown();
        try {
            boolean b = engineExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!b) {
                engineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> long participate(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        dataSource.registerSocketToRoom(socket, roomId);
        return roomId;
    }

    public <T> void leave(Socket socket, T context) {
        RoomSelector<T> roomSelector = engineConfig.getRoomSelector();
        long roomId = roomSelector.selectRoom(socket, context);
        dataSource.removeSocketFromRoom(socket, roomId);
    }
}
