package com.jwt.server.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void validUsername() {
        assertNull(ValidationUtils.validateUsername("john"));
        assertNull(ValidationUtils.validateUsername("john_doe"));
        assertNull(ValidationUtils.validateUsername("john-doe"));
        assertNull(ValidationUtils.validateUsername("a1b2c3"));
    }

    @Test
    void nullUsername() {
        assertNotNull(ValidationUtils.validateUsername(null));
    }

    @Test
    void shortUsername() {
        assertNotNull(ValidationUtils.validateUsername("ab"));
    }

    @Test
    void longUsername() {
        assertNotNull(ValidationUtils.validateUsername("a".repeat(31)));
    }

    @Test
    void usernameWithSpecialChars() {
        assertNotNull(ValidationUtils.validateUsername("john@doe"));
        assertNotNull(ValidationUtils.validateUsername("john doe"));
    }

    @Test
    void validPassword() {
        assertNull(ValidationUtils.validatePassword("password123"));
        assertNull(ValidationUtils.validatePassword("a".repeat(8)));
        assertNull(ValidationUtils.validatePassword("a".repeat(128)));
    }

    @Test
    void nullPassword() {
        assertNotNull(ValidationUtils.validatePassword(null));
    }

    @Test
    void shortPassword() {
        assertNotNull(ValidationUtils.validatePassword("a".repeat(7)));
    }

    @Test
    void longPassword() {
        assertNotNull(ValidationUtils.validatePassword("a".repeat(129)));
    }

    @Test
    void emptyPassword() {
        assertNotNull(ValidationUtils.validatePassword(""));
    }
}
