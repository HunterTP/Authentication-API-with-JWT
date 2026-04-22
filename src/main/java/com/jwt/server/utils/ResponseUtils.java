package com.jwt.server.utils;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

public class ResponseUtils {
    // Sends a JSON response with the given status code and message
    public static void send(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    // Sends an error response with the given status code and message
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        send(exchange, statusCode, "{\"error\": \"" + message + "\"}");
    }
}