package io.github.youseonghyeon.broadcast.kafka;

/// 동일그룹필터링을 위한 Kafka 메시지 래퍼 클래스입니다.
public record KafkaDataWrapper<E>(String topic, String groupId, E message) {
}
