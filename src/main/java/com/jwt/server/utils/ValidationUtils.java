package com.jwt.server.utils;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private ValidationUtils() {}

    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        String trimmed = username.trim();
        if (trimmed.length() < Config.USERNAME_MIN_LENGTH) {
            return "Username must be at least " + Config.USERNAME_MIN_LENGTH + " characters";
        }
        if (trimmed.length() > Config.USERNAME_MAX_LENGTH) {
            return "Username must be at most " + Config.USERNAME_MAX_LENGTH + " characters";
        }
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            return "Username may only contain letters, numbers, hyphens and underscores";
        }
        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < Config.PASSWORD_MIN_LENGTH) {
            return "Password must be at least " + Config.PASSWORD_MIN_LENGTH + " characters";
        }
        if (password.length() > Config.PASSWORD_MAX_LENGTH) {
            return "Password must be at most " + Config.PASSWORD_MAX_LENGTH + " characters";
        }
        return null;
    }
}
