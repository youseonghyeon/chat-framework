package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;
import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.SendFilterPolicy;
import io.github.youseonghyeon.engine.config.MessageWriter;
import io.github.youseonghyeon.engine.exception.NoParticipantException;
import io.github.youseonghyeon.session.InMemorySessionStore;

import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 채팅 메시지 전송을 담당하는 클래스입니다.
 *
 * <p>{@link InMemorySessionStore}를 기반으로 룸에 속한 클라이언트 소켓을 조회하고,
 * {@link SendFilterPolicy}로 수신 대상을 필터링한 뒤,
 * {@link MessageWriter}를 이용해 메시지를 직렬화 및 전송합니다.</p>
 *
 * <p>내부적으로 {@link ThreadPoolExecutor}를 사용하여 비동기 전송을 수행하므로,
 * 송신 과정에서 호출자의 흐름이 차단되지 않습니다.</p>
 *
 * <p>주요 책임:
 * <ul>
 *   <li>송신 대상 소켓 필터링</li>
 *   <li>OutputStream 기반 메시지 전송</li>
 *   <li>송신 실패 예외 처리 (로깅, 모니터링 연계 등)</li>
 * </ul>
 * </p>
 *
 * <p>사용 예:
 * <pre>{@code
 * chatManager.send(socket, roomId, message, new JsonMessageWriter<>());
 * }</pre>
 * </p>
 */
public class ChatManager {

    private final InMemorySessionStore inMemorySessionStore;
    private final ThreadPoolExecutor executor;
    private final SendFilterPolicy filterPolicy;
    private final MessageBroadcaster broadcaster;

    /**
     * ChatManager 인스턴스를 생성합니다.
     *
     * @param inMemorySessionStore 룸-소켓 매핑을 담당하는 인메모리 세션 저장소
     * @param executor 메시지 전송을 처리할 비동기 스레드 풀
     * @param filterPolicy 메시지 송신 여부를 판단하는 필터 정책
     * @param broadcaster 메시지 후속 브로드캐스트 처리기 (예: 로그, 이벤트 발행 등)
     */
    public ChatManager(InMemorySessionStore inMemorySessionStore,
                       ThreadPoolExecutor executor,
                       SendFilterPolicy filterPolicy,
                       MessageBroadcaster broadcaster) {
        this.inMemorySessionStore = inMemorySessionStore;
        this.executor = executor;
        this.filterPolicy = filterPolicy;
        this.broadcaster = broadcaster;
    }

    /**
     * 주어진 소켓과 룸 ID를 기반으로, 해당 룸에 메시지를 전송합니다.
     * 송신 대상은 {@link SendFilterPolicy}에 의해 필터링됩니다.
     *
     * @param self 송신자 소켓
     * @param roomId 메시지를 전송할 룸 ID
     * @param message 전송할 메시지 객체
     * @param messageWriter 메시지를 직렬화하여 OutputStream에 쓰는 전략
     * @param <T> 메시지 타입
     * @throws NoParticipantException 대상이 없을 경우 예외를 던질 수 있음 (현재는 생략됨)
     */
    public <T> void send(Socket self, Long roomId, T message, MessageWriter<T> messageWriter) {
        List<Socket> targetSockets = extractTargetSockets(self, roomId);

        executor.submit(() -> broadcaster.broadcast(roomId, message));

        if (targetSockets.isEmpty()) {
            // 향후: 예외를 던지거나 로깅 또는 fallback 동작 추가 가능
            // throw new NoParticipantException(roomId);
            return;
        }

        for (Socket socket : targetSockets) {
            executor.submit(() -> writeMessage(socket, message, messageWriter));
        }
    }

    /**
     * 룸 내의 모든 소켓 중 필터 정책에 따라 송신 대상만 추출합니다.
     *
     * @param self 송신자 소켓
     * @param roomId 룸 ID
     * @return 송신 대상 소켓 목록
     */
    private List<Socket> extractTargetSockets(Socket self, Long roomId) {
        Set<Socket> sockets = inMemorySessionStore.findRoomBy(roomId);
        return sockets.stream()
                .filter(socket -> filterPolicy.shouldSend(socket, self))
                .toList();
    }

    /**
     * 주어진 소켓에 메시지를 직렬화하여 전송합니다.
     * <p>예외가 발생해도 호출자에게 전달되지 않고 내부에서 처리됩니다.</p>
     *
     * @param socket 대상 소켓
     * @param message 전송할 메시지
     * @param messageWriter 메시지 직렬화 전략
     * @param <T> 메시지 타입
     */
    private static <T> void writeMessage(Socket socket, T message, MessageWriter<T> messageWriter) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            messageWriter.write(message, outputStream);
        } catch (Exception e) {
            // TODO: 외부 로깅 시스템과 연계하거나, 사용자 정의 에러 핸들러 연결 고려
            System.err.println("Failed to send message to socket: " + socket);
            e.printStackTrace();
        }
    }
}
