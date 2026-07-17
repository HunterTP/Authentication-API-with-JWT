package com.jwt.server.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import com.jwt.server.config.Config;
import com.jwt.server.persistence.TokenBlacklist;

public class JwtUtils {

    private static final SecretKey SECRET_KEY;

    static {
        String secret = Config.JWT_SECRET;

        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                "JWT_SECRET environment variable is not set! " +
                "Set a random secret with at least 32 characters.");
        }

        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long!");
        }

        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + Config.JWT_EXPIRATION_MS))
            .signWith(SECRET_KEY, Jwts.SIG.HS256)
            .compact();
    }

    public static String extractUsername(String token) {
        if (TokenBlacklist.isBlacklisted(token)) {
            throw new RuntimeException("Token has been invalidated");
        }
        return Jwts.parser()
            .verifyWith(SECRET_KEY)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
}
