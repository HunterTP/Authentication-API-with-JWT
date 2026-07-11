package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.RequestUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.TokenBlacklist;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdateUsernameHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(UpdateUsernameHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsUtils.addCorsHeaders(exchange);
        if (CorsUtils.handleOptionsRequest(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equals("PUT")) {
            ResponseUtils.sendError(exchange, 405, "Only PUT method is allowed");
            return;
        }

        if (!RequestUtils.isJsonContentType(exchange)) {
            ResponseUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ResponseUtils.sendError(exchange, 401, "No Token provided");
            return;
        }

        String token = authHeader.substring(7);

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String newUsername = JsonUtils.extractValue(body, "username");

        if (newUsername == null) {
            ResponseUtils.sendError(exchange, 400, "username is required");
            return;
        }

        String userError = ValidationUtils.validateUsername(newUsername);
        if (userError != null) {
            ResponseUtils.sendError(exchange, 400, userError);
            return;
        }

        String oldUsername;
        try {
            oldUsername = JwtUtils.extractUsername(token);
        } catch (Exception e) {
            ResponseUtils.sendError(exchange, 401, "Invalid token");
            return;
        }

        try {
            boolean updated = SqlUtils.updateUsername(oldUsername, newUsername);
            if (!updated) {
                ResponseUtils.sendError(exchange, 404, "User not found");
                log.warn("Username update failed: {} not found", oldUsername);
                return;
            }
            TokenBlacklist.invalidate(token);
            String response = "{\"message\": \"Username updated\"}";
            ResponseUtils.send(exchange, 200, response);
            log.info("Username changed from {} to {}", oldUsername, newUsername);
        } catch (Exception e) {
            if (e.getMessage().contains("duplicate username")) {
                ResponseUtils.sendError(exchange, 409, "Username already exists");
                return;
            }
            log.error("Username update failed: {}", e.getMessage());
            ResponseUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
