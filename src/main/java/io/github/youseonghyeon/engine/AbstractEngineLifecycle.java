package io.github.youseonghyeon.engine;

abstract class AbstractEngineLifecycle {

    private boolean started = false;

    public void start() {
        if (started) {
            throw new IllegalStateException("Engine is already started.");
        }
        initThreadPool();
        initResource();
        initRoomSelectorIfAbsent();
        started = true;
    }

    public void stop() {
        if (!started) {
            throw new IllegalStateException("Engine is not started yet.");
        }
        stopThreadPool();
        started = false;
    }

    protected abstract void initResource();
    protected abstract void initThreadPool();
    protected abstract void initRoomSelectorIfAbsent();
    protected abstract void stopThreadPool();

    protected int reasonableQueueSize(int maxThreadPoolSize) {
        return Math.max(maxThreadPoolSize * 10, 100);
    }

}
