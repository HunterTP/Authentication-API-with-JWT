package com.jwt.server.utils;

public final class Config {

    private Config() {}

    // Server
    public static final int PORT = Integer.parseInt(System.getenv().getOrDefault("API_PORT", "8443"));

    // JWT
    public static final String JWT_SECRET = System.getenv("JWT_SECRET");
    public static final long JWT_EXPIRATION_MS = Long.parseLong(
        System.getenv().getOrDefault("JWT_EXPIRATION_MS", "3600000"));

    // BCrypt
    public static final int BCRYPT_WORKLOAD = Integer.parseInt(
        System.getenv().getOrDefault("BCRYPT_WORKLOAD", "12"));
    public static final String BCRYPT_PEPPER = System.getenv("BCRYPT_PEPPER");

    // Keystore
    public static final String KEYSTORE_PATH = System.getenv("KEYSTORE_PATH");
    public static final String KEYSTORE_PASS = System.getenv().getOrDefault("KEYSTORE_PASS", "123456");
    public static final String KEY_PASS = System.getenv().getOrDefault("KEY_PASS", "123456");

    // Validation
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 30;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 128;

    // Rate Limiting
    public static final int RATE_LIMIT_MAX_REQUESTS = 20;
    public static final long RATE_LIMIT_WINDOW_MS = 60_000;

    // Account Jailing
    public static final int ACCOUNT_MAX_ATTEMPTS = 5;
    public static final long ACCOUNT_LOCK_DURATION_MS = 900_000;
}
