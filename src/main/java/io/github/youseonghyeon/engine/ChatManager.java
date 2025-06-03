package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.engine.config.ChattingEngineConfig;
import io.github.youseonghyeon.engine.config.SendFilterPolicy;
import io.github.youseonghyeon.engine.config.SendParser;
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
 * <p>{@link InMemorySessionStore}로부터 룸에 속한 클라이언트 소켓들을 조회하고,
 * 설정된 {@link SendFilterPolicy}에 따라 송신 대상 소켓을 필터링한 후,
 * 사용자 정의 {@link SendParser}를 통해 메시지를 전송합니다.</p>
 *
 * <p>내부적으로 {@link ThreadPoolExecutor}를 사용하여 비동기적으로 메시지를 전송하므로,
 * 전송 작업이 호출자의 흐름을 차단하지 않습니다.</p>
 *
 * <p>주요 책임:
 * <ul>
 *   <li>메시지 수신자의 필터링</li>
 *   <li>메시지 직렬화 및 OutputStream 전송</li>
 *   <li>전송 실패에 대한 예외 로깅 (향후 모니터링 연계 가능)</li>
 * </ul>
 * </p>
 *
 * <p>사용 예:
 * <pre>{@code
 * chatManager.send(socket, roomId, message, new MessageParser<message type>());
 * }</pre>
 * </p>
 *
 * @throws NoParticipantException 룸에 송신 대상이 없을 경우 예외 발생
 */
public class ChatManager {

    private final InMemorySessionStore inMemorySessionStore;
    private final ThreadPoolExecutor executor;
    private final SendFilterPolicy filterPolicy;

    public ChatManager(InMemorySessionStore inMemorySessionStore, ThreadPoolExecutor executor, ChattingEngineConfig config) {
        this.inMemorySessionStore = inMemorySessionStore;
        this.executor = executor;
        this.filterPolicy = config.getSendFilterPolicy();
    }

    public <T> void send(Socket self, Long roomId, T message, SendParser<T> parser) throws NoParticipantException {
        Set<Socket> sockets = inMemorySessionStore.findRoomBy(roomId);
        List<Socket> targetSockets = sockets.stream().filter(socket -> filterPolicy.shouldSend(socket, self)).toList();
        if (targetSockets.isEmpty()) {
            throw new NoParticipantException("No participants in the room: " + roomId);
        }

        for (Socket socket : targetSockets) {
            executor.execute(() -> {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    parser.write(message, outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

}
