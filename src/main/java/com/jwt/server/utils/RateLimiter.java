package com.jwt.server.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimiter {

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requests = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimiter() {
        this(Config.RATE_LIMIT_MAX_REQUESTS, Config.RATE_LIMIT_WINDOW_MS);
    }

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps = requests.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
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
}
