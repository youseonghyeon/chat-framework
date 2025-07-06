package io.github.youseonghyeon.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockCoordinator {

    private LockCoordinator() {
    }

    public static void withLock(Runnable runnable, ReentrantLock instance, int lockSeconds) {
        try {
            boolean tryLock = instance.tryLock(lockSeconds, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new IllegalStateException("Failed to acquire lock within " + lockSeconds + " seconds.");
            }
            try {
                runnable.run();
            } finally {
                instance.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public static <T> T withLock(Callable<T> callable, ReentrantLock instance, int lockSeconds) {
        try {
            boolean tryLock = instance.tryLock(lockSeconds, TimeUnit.SECONDS);
            if (!tryLock) {
                throw new IllegalStateException("Failed to acquire lock within " + lockSeconds + " seconds.");
            }
            try {
                return callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                instance.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
