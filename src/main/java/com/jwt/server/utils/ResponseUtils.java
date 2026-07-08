package com.jwt.server.utils;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class ResponseUtils {

    public static void addSecurityHeaders(Headers headers) {
        headers.set("Content-Type", "application/json");
        headers.set("Strict-Transport-Security", "max-age=63072000; includeSubDomains");
        headers.set("Content-Security-Policy", "default-src 'none'");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
    }

    public static void send(HttpExchange exchange, int statusCode, String response) throws IOException {
        addSecurityHeaders(exchange.getResponseHeaders());
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String escaped = message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
        send(exchange, statusCode, "{\"error\":\"" + escaped + "\"}");
    }
}