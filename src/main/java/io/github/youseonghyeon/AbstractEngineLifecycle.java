package io.github.youseonghyeon;

abstract class AbstractEngineLifecycle {

    private boolean started = false;

    public void start() {
        if (started) {
            throw new IllegalStateException("Engine is already started.");
        }
        initServerSocket();
        initThreadPool();
        initResource();
        started = true;
    }

    public void stop() {
        if (!started) {
            throw new IllegalStateException("Engine is not started yet.");
        }
        closeServerSocket();
        stopThreadPool();
        started = false;
    }

    protected abstract void initServerSocket();
    protected abstract void initResource();
    protected abstract void initThreadPool();

    protected abstract void closeServerSocket();
    protected abstract void stopThreadPool();

    protected int reasonableQueueSize(int maxThreadPoolSize) {
        return Math.max(maxThreadPoolSize * 10, 100);
    }

}
