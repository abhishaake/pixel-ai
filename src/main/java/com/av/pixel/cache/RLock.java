package com.av.pixel.cache;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RLock {

    private final Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    public boolean tryLock(String key, long timeoutMillis) {
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());

        try {
            return lock.tryLock(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(String key) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
