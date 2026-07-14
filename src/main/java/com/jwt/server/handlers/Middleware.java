package com.jwt.server.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.RequestUtils;
import com.jwt.server.utils.ResponseUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Middleware {

    private static final Logger log = LoggerFactory.getLogger(Middleware.class);

    public static HttpHandler wrap(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;
            if (!isMethodAllowed(exchange, allowedMethods)) return;
            if (!requireJsonContentType(exchange)) return;

            handleSafe(exchange, handler);
        };
    }

    public static HttpHandler wrapProtected(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;
            if (!isMethodAllowed(exchange, allowedMethods)) return;
            if (!requireJsonContentType(exchange)) return;

            String token = extractToken(exchange);
            if (token == null) {
                ResponseUtils.sendError(exchange, 401, "No Token provided");
                return;
            }

            String username;
            try {
                username = JwtUtils.extractUsername(token);
            } catch (Exception e) {
                ResponseUtils.sendError(exchange, 401, "Invalid token");
                return;
            }

            exchange.setAttribute("token", token);
            exchange.setAttribute("username", username);
            handleSafe(exchange, handler);
        };
    }

    private static void handleSafe(HttpExchange exchange, HttpHandler handler) throws IOException {
        try {
            handler.handle(exchange);
        } catch (Exception e) {
            log.error("Unhandled error: {}", e.getMessage());
            ResponseUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }

    private static boolean isMethodAllowed(HttpExchange exchange, String... allowedMethods) throws IOException {
        String method = exchange.getRequestMethod();
        for (String m : allowedMethods) {
            if (m.equalsIgnoreCase(method)) return true;
        }
        ResponseUtils.sendError(exchange, 405, "Only " + String.join(", ", allowedMethods) + " allowed");
        return false;
    }

    private static boolean requireJsonContentType(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) return true;
        if (!RequestUtils.isJsonContentType(exchange)) {
            ResponseUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return false;
        }
        return true;
    }

    private static String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}
