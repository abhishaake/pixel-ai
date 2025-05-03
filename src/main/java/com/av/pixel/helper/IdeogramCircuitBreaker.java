package com.av.pixel.helper;

import com.av.pixel.exception.IdeogramServerException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class IdeogramCircuitBreaker {

    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final int threshold;
    private final long resetTimeoutMs;

    public IdeogramCircuitBreaker (int threshold, long resetTimeoutMs) {
        this.threshold = threshold;
        this.resetTimeoutMs = resetTimeoutMs;
    }

    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        // Check if circuit is open
        if (failures.get() >= threshold) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            if (timeSinceLastFailure < resetTimeoutMs) {
                return fallback.get(); // Circuit open, use fallback
            }
            // Attempt reset after timeout
            failures.set(0);
        }

        // Circuit closed or attempting reset, try the call
        try {
            T result = action.get();
            // Success, reset failures
            failures.set(0);
            return result;
        } catch (IdeogramServerException e) {
            // Record failure
            failures.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            return fallback.get();
        }
    }
}
