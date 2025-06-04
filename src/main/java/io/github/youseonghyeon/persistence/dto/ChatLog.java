package io.github.youseonghyeon.persistence.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 채팅 로그 정보를 저장하는 DTO 클래스입니다.
 * DB 저장 또는 로그 처리용으로 사용됩니다.
 */
public record ChatLog(long roomId, long timestamp, String sender, String message) {

    public LocalDateTime toLocalDateTime() {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
