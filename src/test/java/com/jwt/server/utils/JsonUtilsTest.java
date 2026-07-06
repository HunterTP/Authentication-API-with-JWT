package com.jwt.server.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void extractSimpleValue() {
        String json = "{\"username\":\"john\",\"password\":\"secret\"}";
        assertEquals("john", JsonUtils.extractValue(json, "username"));
        assertEquals("secret", JsonUtils.extractValue(json, "password"));
    }

    @Test
    void extractValueWithSpaces() {
        String json = "{  \"key\"  :  \"value\"  }";
        assertEquals("value", JsonUtils.extractValue(json, "key"));
    }

    @Test
    void extractMissingKey() {
        String json = "{\"a\":\"1\"}";
        assertNull(JsonUtils.extractValue(json, "b"));
    }

    @Test
    void extractValueEmptyJson() {
        assertNull(JsonUtils.extractValue("{}", "key"));
    }

    @Test
    void extractNumericValue() {
        String json = "{\"port\": 8443}";
        assertEquals("8443", JsonUtils.extractValueN(json, "port"));
    }

    @Test
    void extractQuotedNumericValue() {
        String json = "{\"port\": \"8443\"}";
        assertEquals("8443", JsonUtils.extractValueN(json, "port"));
    }

    @Test
    void extractMissingKeyN() {
        String json = "{\"a\":\"1\"}";
        assertNull(JsonUtils.extractValueN(json, "b"));
    }
}
