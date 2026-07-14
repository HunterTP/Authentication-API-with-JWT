package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.TokenBlacklist;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdatePasswordHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(UpdatePasswordHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = (String) exchange.getAttribute("token");

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String newPassword = JsonUtils.extractValue(body, "password");

        if (newPassword == null) {
            ResponseUtils.sendError(exchange, 400, "password is required");
            return;
        }

        String passError = ValidationUtils.validatePassword(newPassword);
        if (passError != null) {
            ResponseUtils.sendError(exchange, 400, passError);
            return;
        }

        String username = (String) exchange.getAttribute("username");

        try {
            boolean updated = SqlUtils.updatePassword(username, newPassword);
            if (!updated) {
                ResponseUtils.sendError(exchange, 404, "User not found");
                log.warn("Password update failed: {} not found", username);
                return;
            }
            TokenBlacklist.invalidate(token);
            String response = "{\"message\": \"Password updated\"}";
            ResponseUtils.send(exchange, 200, response);
            log.info("Password updated for {}", username);
        } catch (Exception e) {
            log.error("Password update failed for {}: {}", username, e.getMessage());
            ResponseUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
