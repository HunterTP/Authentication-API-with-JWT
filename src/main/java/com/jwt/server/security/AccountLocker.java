package com.jwt.server.security;

import java.util.concurrent.ConcurrentHashMap;

import com.jwt.server.config.Config;

public class AccountLocker {

    private static final AccountLocker GLOBAL = new AccountLocker();

    public static AccountLocker global() {
        return GLOBAL;
    }

    public static void cleanupAll() {
        GLOBAL.cleanup();
    }

    private final ConcurrentHashMap<String, LockState> locks = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockDurationMs;

    public AccountLocker() {
        this.maxAttempts = Config.ACCOUNT_MAX_ATTEMPTS;
        this.lockDurationMs = Config.ACCOUNT_LOCK_DURATION_MS;
    }

    public boolean isLocked(String username) {
        LockState state = locks.get(username);
        if (state == null || state.lockedUntil() == 0) return false;
        if (System.currentTimeMillis() >= state.lockedUntil()) {
            locks.remove(username);
            return false;
        }
        return true;
    }

    public void recordFailedAttempt(String username) {
        locks.compute(username, (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null) {
                return new LockState(1, 0);
            }
            if (existing.lockedUntil() > 0 && now >= existing.lockedUntil()) {
                return new LockState(1, 0);
            }
            int attempts = existing.attempts() + 1;
            if (attempts >= maxAttempts) {
                return new LockState(attempts, now + lockDurationMs);
            }
            return new LockState(attempts, 0);
        });
    }

    public void reset(String username) {
        locks.remove(username);
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        locks.entrySet().removeIf(entry -> {
            LockState state = entry.getValue();
            return state.lockedUntil() == 0 || now >= state.lockedUntil();
        });
    }

    private record LockState(int attempts, long lockedUntil) {}
}
