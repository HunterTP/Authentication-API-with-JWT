package com.jwt.server.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void defaultPort() {
        assertNotNull(Config.PORT);
    }

    @Test
    void defaultJwtExpiration() {
        assertEquals(3600000L, Config.JWT_EXPIRATION_MS);
    }

    @Test
    void defaultBcryptWorkload() {
        assertTrue(Config.BCRYPT_WORKLOAD > 0);
    }

    @Test
    void validationConstants() {
        assertTrue(Config.USERNAME_MIN_LENGTH > 0);
        assertTrue(Config.USERNAME_MAX_LENGTH > Config.USERNAME_MIN_LENGTH);
        assertTrue(Config.PASSWORD_MIN_LENGTH > 0);
        assertTrue(Config.PASSWORD_MAX_LENGTH > Config.PASSWORD_MIN_LENGTH);
    }

    @Test
    void rateLimitConstants() {
        assertTrue(Config.RATE_LIMIT_MAX_REQUESTS > 0);
        assertTrue(Config.RATE_LIMIT_WINDOW_MS > 0);
    }
}
