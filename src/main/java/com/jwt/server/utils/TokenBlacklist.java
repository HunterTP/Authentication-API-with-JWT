package com.jwt.server.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TokenBlacklist {

    private static final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();
    private static final long TOKEN_TTL_MS = 86_400_000;

    public static void init() {
        cache.clear();
        try {
            Map<String, Long> stored = SqlUtils.loadBlacklistedTokens();
            cache.putAll(stored);
        } catch (Exception e) {
            // DB not available yet — will be loaded on first access
        }
    }

    public static void invalidate(String token) {
        long expiry = System.currentTimeMillis() + TOKEN_TTL_MS;
        cache.put(token, expiry);
        try {
            SqlUtils.persistBlacklistedToken(token, expiry);
        } catch (Exception e) {
            // non-critical
        }
    }

    public static boolean isBlacklisted(String token) {
        Long expiry = cache.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            cache.remove(token);
            return false;
        }
        return true;
    }

    public static void cleanup() {
        try {
            SqlUtils.removeExpiredBlacklistedTokens();
        } catch (Exception e) {
            // non-critical
        }
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> e.getValue() < now);
    }
}
