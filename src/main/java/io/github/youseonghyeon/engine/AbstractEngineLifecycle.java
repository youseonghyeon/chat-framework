package io.github.youseonghyeon.engine;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 채팅 엔진 생명주기 추상 클래스.
 *
 * <p>이 클래스는 채팅 엔진의 시작(start)과 종료(stop) 과정에서 필요한 자원 초기화 및
 * 정리를 담당하는 공통 생명주기 로직을 제공합니다. 자식 클래스는 구체적인 초기화 및 정리 작업을
 * 메서드 오버라이딩을 통해 구현해야 합니다.</p>
 *
 * <p>스레드 안전성을 위해 내부적으로 {@link ReentrantLock}을 사용하여 start/stop 동작이
 * 동시 실행되지 않도록 보호합니다.</p>
 *
 * @author 유성현
 */
abstract class AbstractEngineLifecycle {

    /// 엔진 상태 변경 시 동시성 제어를 위한 락입니다.
    private final ReentrantLock lock = new ReentrantLock(false);
    /// 엔진이 이미 시작되었는지를 나타내는 플래그입니다.
    private volatile boolean started = false;

    final int lockTimeout = 10;

    /**
     * 엔진을 시작합니다. 내부적으로 스레드 풀, 리소스, 룸 셀렉터를 초기화하며,
     * 중복 시작을 방지하기 위해 락을 사용합니다.
     *
     * @throws IllegalStateException 이미 시작된 경우 또는 락 획득에 실패한 경우
     */
    public void start() {
        withLock(() -> {
            if (started) {
                throw new IllegalStateException("Engine is already started.");
            }
            initThreadPool();
            initResource();
            initDefaultConfigIfAbsent();
            started = true;
        });
    }

    /**
     * 엔진을 종료합니다. 내부적으로 스레드 풀을 정리하며, 모든 세션을 종료합니다.
     * 시작되지 않은 상태에서의 종료를 방지합니다.
     *
     * @throws IllegalStateException 아직 시작되지 않았거나 락 획득에 실패한 경우
     */
    public void stop() {
        withLock(() -> {
            if (!started) {
                throw new IllegalStateException("Engine is not started yet.");
            }
            stopThreadPool();
            destroyAllSessions();
            started = false;
        });
    }

    /**
     * start() 또는 stop() 실행 시 동기화를 보장하기 위해 락을 사용하여
     * 주어진 작업을 실행합니다.
     *
     * @param runnable 실행할 작업
     * @throws RuntimeException 락 획득 중 인터럽트가 발생한 경우
     */
    private void withLock(Runnable runnable) {
        try {
            boolean tryLock = lock.tryLock(lockTimeout, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new IllegalStateException("Failed to acquire lock within 10 seconds.");
            }
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 자원 초기화를 위한 메서드입니다.
     * 예: 세션 저장소, 브로드캐스트 핸들러 등
     */
    protected abstract void initResource();

    /**
     * 스레드 풀 초기화를 위한 메서드입니다.
     * 예: 메시지 전송용 ExecutorService
     */
    protected abstract void initThreadPool();

    /**
     * Config 가 설정되지 않았을 경우 기본 구현을 설정하기 위한 메서드입니다.
     */
    protected abstract void initDefaultConfigIfAbsent();

    /**
     * 스레드 풀 종료를 위한 메서드입니다.
     */
    protected abstract void stopThreadPool();

    /**
     * 모든 세션을 종료하는 메서드입니다.
     * 예: 서버 소켓을 닫거나, 세션 저장소의 모든 소켓을 종료합니다.
     */
    protected abstract void destroyAllSessions();

    /**
     * 최대 쓰레드 수를 기반으로 적절한 큐 크기를 반환합니다.
     *
     * @param maxThreadPoolSize 최대 쓰레드 수
     * @return 큐 크기
     */
    protected int reasonableQueueSize(int maxThreadPoolSize) {
        return Math.max(maxThreadPoolSize * 10, 100);
    }

    protected <T> T withDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }


}
