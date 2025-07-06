package io.github.youseonghyeon.config;

import java.nio.channels.SocketChannel;
import java.util.Objects;

/**
 * 메시지 송신 대상 필터링을 위한 정책 인터페이스입니다.
 *
 * <p>이 인터페이스는 송신자-수신자 소켓 쌍에 대해 메시지를 전송해야 할지를 판단하는
 * 단순한 불리언 기반 조건을 정의합니다.</p>
 *
 * <p>기본적으로 {@code and()}, {@code or()}, {@code negate()} 등을 통해
 * 필터 정책을 조합할 수 있는 디폴트 메서드를 제공합니다.</p>
 *
 * <p>예시:
 * <pre>{@code
 * SendFilterPolicy policy = socketIsAlive().and(notSelf());
 * }</pre>
 * </p>
 *
 * @author 유성현
 * @since preview-1.0.0
 */
@FunctionalInterface
public interface SendFilterPolicy {

    /**
     * 주어진 송신자-수신자 소켓 쌍에 대해 메시지를 전송할지를 결정합니다.
     *
     * @param receiver 수신자 소켓
     * @param sender   송신자 소켓
     * @return true일 경우 메시지를 전송함
     */
    boolean shouldSend(SocketChannel receiver, SocketChannel sender);

    /**
     * 현재 정책과 주어진 정책을 논리 AND로 결합합니다.
     *
     * @param other 결합할 다른 필터 정책
     * @return 두 조건을 모두 만족할 때만 true를 반환하는 결합 정책
     */
    default SendFilterPolicy and(SendFilterPolicy other) {
        Objects.requireNonNull(other);
        return (receiver, sender) ->
                this.shouldSend(receiver, sender)
                && other.shouldSend(receiver, sender);
    }

    /**
     * 현재 정책을 부정합니다 (NOT).
     *
     * @return 현재 조건의 부정 조건을 반환하는 정책
     */
    default SendFilterPolicy negate() {
        return (receiver, sender) ->
                !this.shouldSend(receiver, sender);
    }

    /**
     * 현재 정책과 주어진 정책을 논리 OR로 결합합니다.
     *
     * @param other 결합할 다른 필터 정책
     * @return 둘 중 하나라도 만족하면 true를 반환하는 결합 정책
     */
    default SendFilterPolicy or(SendFilterPolicy other) {
        Objects.requireNonNull(other);
        return (receiver, sender) ->
                this.shouldSend(receiver, sender)
                || other.shouldSend(receiver, sender);
    }

    /**
     * 주어진 정책을 NOT으로 감싼 부정 정책을 생성합니다.
     * <p>이는 {@code target.negate()}와 동일합니다.</p>
     *
     * @param target 부정할 대상 정책
     * @return 부정된 정책
     */
    default SendFilterPolicy not(SendFilterPolicy target) {
        Objects.requireNonNull(target);
        return target.negate();
    }


    /**
     * 송신자 본인을 제외하고 메시지를 브로드캐스트하는 기본 필터입니다.
     */
    class BroadcastExceptSelf implements SendFilterPolicy {
        @Override
        public boolean shouldSend(SocketChannel receiver, SocketChannel sender) {
            return !receiver.equals(sender);
        }
    }

    /**
     * 양쪽 소켓이 연결 상태일 때만 메시지를 송신하는 필터입니다.
     * 기본적으로 모든 필터 체인 앞단에 적용됩니다.
     */
    class NotConnected implements SendFilterPolicy {
        @Override
        public boolean shouldSend(SocketChannel receiver, SocketChannel sender) {
            return receiver.isConnected() && sender.isConnected();
        }
    }
}
