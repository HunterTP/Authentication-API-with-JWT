package com.jwt.server.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

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
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        send(exchange, statusCode, "{\"error\":\"" + jsonEscape(message) + "\"}");
    }

    public static void sendMessage(HttpExchange exchange, int statusCode, String message) throws IOException {
        send(exchange, statusCode, "{\"message\":\"" + jsonEscape(message) + "\"}");
    }

    public static String jsonEscape(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
