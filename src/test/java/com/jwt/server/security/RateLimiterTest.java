package com.jwt.server.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void allowWithinLimit() {
        RateLimiter limiter = new RateLimiter(5, 60_000);
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.isAllowed("test"));
        }
    }

    @Test
    void blockAfterLimit() {
        RateLimiter limiter = new RateLimiter(3, 60_000);
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.isAllowed("test"));
        }
        assertFalse(limiter.isAllowed("test"));
    }

    @Test
    void differentKeysIndependent() {
        RateLimiter limiter = new RateLimiter(2, 60_000);
        assertTrue(limiter.isAllowed("a"));
        assertTrue(limiter.isAllowed("a"));
        assertFalse(limiter.isAllowed("a"));
        assertTrue(limiter.isAllowed("b"));
    }

    @Test
    void resetClearsCounter() {
        RateLimiter limiter = new RateLimiter(1, 60_000);
        assertTrue(limiter.isAllowed("test"));
        assertFalse(limiter.isAllowed("test"));
        limiter.reset("test");
        assertTrue(limiter.isAllowed("test"));
    }
}
