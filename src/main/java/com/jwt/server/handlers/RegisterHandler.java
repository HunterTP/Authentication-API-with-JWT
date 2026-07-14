package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.RateLimiter;
import com.jwt.server.utils.RequestUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);
    private static final RateLimiter rateLimiter = RateLimiter.global();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!RequestUtils.isJsonContentType(exchange)) {
            ResponseUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        String clientIp = exchange.getRemoteAddress() != null
            ? exchange.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        if (!rateLimiter.isAllowed(clientIp)) {
            log.warn("Rate limit exceeded for IP: {} on register", clientIp);
            ResponseUtils.sendError(exchange, 429, "Too many requests. Please try again later.");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String username = JsonUtils.extractValue(body, "username");
        String password = JsonUtils.extractValue(body, "password");

        if (username == null || password == null) {
            ResponseUtils.sendError(exchange, 400, "username and password are required");
            return;
        }

        String userError = ValidationUtils.validateUsername(username);
        if (userError != null) {
            ResponseUtils.sendError(exchange, 400, userError);
            return;
        }

        String passError = ValidationUtils.validatePassword(password);
        if (passError != null) {
            ResponseUtils.sendError(exchange, 400, passError);
            return;
        }

        try {
            if (SqlUtils.registerUser(username, password)) {
                String response = "{\"message\": \"User " + username + " was created\"}";
                ResponseUtils.send(exchange, 201, response);
                log.info("User registered: {}", username);
            } else {
                ResponseUtils.sendError(exchange, 500, "Internal Server Error");
            }
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate entry")) {
                ResponseUtils.sendError(exchange, 409, "Username already exists");
            } else {
                log.error("Registration failed: {}", e.getMessage());
                ResponseUtils.sendError(exchange, 500, "Internal Server Error");
            }
        }
    }
}
