package com.jwt.server.middleware;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.exception.HttpException;
import com.jwt.server.http.RequestUtils;
import com.jwt.server.http.ResponseUtils;
import com.jwt.server.security.CorsUtils;
import com.jwt.server.security.JwtUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Middleware {

    private static final Logger log = LoggerFactory.getLogger(Middleware.class);

    public static HttpHandler wrap(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;
            try {
                ensureMethodAllowed(exchange, allowedMethods);
                ensureJsonContentType(exchange);
                handleSafe(exchange, handler);
            } catch (HttpException e) {
                log.warn("{} {}", e.getStatusCode(), e.getMessage());
                quietlySendError(exchange, e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error in wrap", e);
                quietlySendError(exchange, 500, "Internal Server Error");
            }
        };
    }

    public static HttpHandler wrapProtected(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;
            try {
                ensureMethodAllowed(exchange, allowedMethods);
                ensureJsonContentType(exchange);

                String token = extractToken(exchange);
                if (token == null) throw new HttpException(401, "No Token provided");

                String username;
                try {
                    username = JwtUtils.extractUsername(token);
                } catch (Exception e) {
                    throw new HttpException(401, "Invalid token");
                }

                exchange.setAttribute("token", token);
                exchange.setAttribute("username", username);
                handleSafe(exchange, handler);
            } catch (HttpException e) {
                log.warn("{} {}", e.getStatusCode(), e.getMessage());
                quietlySendError(exchange, e.getStatusCode(), e.getMessage());
            } catch (Exception e) {
                log.error("Unexpected error in wrapProtected", e);
                quietlySendError(exchange, 500, "Internal Server Error");
            }
        };
    }

    private static void handleSafe(HttpExchange exchange, HttpHandler handler) throws IOException {
        try {
            handler.handle(exchange);
        } catch (HttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unhandled error in handler", e);
            throw new HttpException(500, "Internal Server Error");
        }
    }

    private static void ensureMethodAllowed(HttpExchange exchange, String... allowedMethods) throws IOException {
        String method = exchange.getRequestMethod();
        for (String m : allowedMethods) {
            if (m.equalsIgnoreCase(method)) return;
        }
        throw new HttpException(405, "Only " + String.join(", ", allowedMethods) + " allowed");
    }

    private static void ensureJsonContentType(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) return;
        if (!RequestUtils.isJsonContentType(exchange)) {
            throw new HttpException(415, "Content-Type must be application/json");
        }
    }

    private static String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }

    private static void quietlySendError(HttpExchange exchange, int statusCode, String message) {
        try {
            ResponseUtils.sendError(exchange, statusCode, message);
        } catch (IOException e) {
            log.warn("Failed to send error response: {}", e.getMessage());
        }
    }
}
