package com.jwt.server.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import com.jwt.server.config.Config;

public class RateLimiter {

    private static final RateLimiter GLOBAL = new RateLimiter();

    public static RateLimiter global() {
        return GLOBAL;
    }

    public static void cleanupAll() {
        GLOBAL.cleanup();
    }

    private final ConcurrentHashMap<String, TimestampWindow> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    private RateLimiter() {
        this(Config.RATE_LIMIT_MAX_REQUESTS, Config.RATE_LIMIT_WINDOW_MS);
    }

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        TimestampWindow tw = requests.computeIfAbsent(key, k -> new TimestampWindow());
        synchronized (tw) {
            Deque<Long> timestamps = tw.timestamps;
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    public void reset(String key) {
        requests.remove(key);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        requests.entrySet().removeIf(entry -> {
            TimestampWindow tw = entry.getValue();
            synchronized (tw) {
                Deque<Long> timestamps = tw.timestamps;
                while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowMs) {
                    timestamps.pollFirst();
                }
                return timestamps.isEmpty();
            }
        });
    }

    private static class TimestampWindow {
        final Deque<Long> timestamps = new ArrayDeque<>();
    }
}
