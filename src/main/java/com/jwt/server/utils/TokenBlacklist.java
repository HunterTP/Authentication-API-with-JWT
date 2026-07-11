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
        String hash = sha256(token);
        long expiry = System.currentTimeMillis() + TOKEN_TTL_MS;
        cache.put(hash, expiry);
        try {
            SqlUtils.persistBlacklistedToken(hash, expiry);
        } catch (Exception e) {
            // non-critical
        }
    }

    public static boolean isBlacklisted(String token) {
        String hash = sha256(token);
        Long expiry = cache.get(hash);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            cache.remove(hash);
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

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
