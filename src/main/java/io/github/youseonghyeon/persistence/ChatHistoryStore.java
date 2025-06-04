package io.github.youseonghyeon.persistence;

import io.github.youseonghyeon.persistence.dto.ChatLog;

import java.util.List;

public interface ChatHistoryStore {
    /**
     * 채팅 메시지를 저장합니다.
     *
     * @param log 저장할 채팅 로그
     */
    void save(ChatLog log);

    /**
     * 특정 채팅방의 최근 메시지들을 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param limit  최대 조회 개수
     * @return 조회된 채팅 로그 목록
     */
    List<ChatLog> findRecentLogs(long roomId, int limit);

    /**
     * 특정 채팅방의 시간 구간 기준 로그를 조회합니다.
     *
     * @param roomId   채팅방 ID
     * @param fromUnix 시작 시간 (Epoch millis)
     * @param toUnix   종료 시간 (Epoch millis)
     * @return 조회된 채팅 로그 목록
     */
    List<ChatLog> findLogsBetween(long roomId, long fromUnix, long toUnix);

    /**
     * 특정 채팅방의 시점 기준으로 과거의 메시지들을 조회합니다.
     *
     * @param roomId 채팅방 ID
     * @param toUnix 조회 기준 시점 (Epoch millis)
     * @param limit  최대 조회 개수
     * @return 조회된 채팅 로그 목록
     */
    List<ChatLog> findLogsBefore(long roomId, long toUnix, int limit);
}
