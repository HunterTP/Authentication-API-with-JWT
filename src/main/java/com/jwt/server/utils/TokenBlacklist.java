package com.jwt.server.utils;

import java.util.concurrent.ConcurrentHashMap;

public class TokenBlacklist {

    private static final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public static void invalidate(String token) {
        blacklist.put(token, System.currentTimeMillis() + 86_400_000);
    }

    public static boolean isBlacklisted(String token) {
        Long expiry = blacklist.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
