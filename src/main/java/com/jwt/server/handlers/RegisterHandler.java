package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsUtils.addCorsHeaders(exchange);
        if (CorsUtils.handleOptionsRequest(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equals("POST")) {
            ResponseUtils.sendError(exchange, 405, "Only POST method is allowed");
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
            SqlUtils.registerUser(exchange, username, password);
            log.info("User registered: {}", username);
        } catch (Exception e) {
            log.error("Registration failed for {}: {}", username, e.getMessage());
            ResponseUtils.sendError(exchange, 500, "Error occurred while registering the user");
        }
    }
}
