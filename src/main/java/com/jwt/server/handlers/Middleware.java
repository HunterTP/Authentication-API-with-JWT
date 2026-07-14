package com.jwt.server.handlers;

import java.io.IOException;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.ResponseUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Middleware {

    public static HttpHandler wrap(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;

            if (!isMethodAllowed(exchange, allowedMethods)) return;

            handler.handle(exchange);
        };
    }

    public static HttpHandler wrapProtected(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) return;

            if (!isMethodAllowed(exchange, allowedMethods)) return;

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
            handler.handle(exchange);
        };
    }

    private static boolean isMethodAllowed(HttpExchange exchange, String... allowedMethods) throws IOException {
        String method = exchange.getRequestMethod();
        for (String m : allowedMethods) {
            if (m.equalsIgnoreCase(method)) return true;
        }
        ResponseUtils.sendError(exchange, 405, "Only " + String.join(", ", allowedMethods) + " allowed");
        return false;
    }

    private static String extractToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }
}
