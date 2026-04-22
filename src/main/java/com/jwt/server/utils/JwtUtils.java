package com.jwt.server.utils;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JwtUtils {
    private static final SecretKey SECRET_KEY;
    static {
        String secret = System.getenv("JWT_SECRET");
        
        // Fallback NUR für Entwicklung!
        if (secret == null || secret.isEmpty()) {
            System.out.println("JWT_SECRET not set in environment variables. Using default secret key.");
            secret = "mySuperSecretKeyForJWTThatIsAtLeast32Chars";
        }
        
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long!");
        }
        
        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate a JWT token for a given username
    public static String generateToken(String username) {
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 Stunde
            .signWith(SECRET_KEY, Jwts.SIG.HS256)
            .compact();
    }
    
    // Extracts the username from a token
    public static String extractUsername(String token) {
        return Jwts.parser()
            .verifyWith(SECRET_KEY)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }
}