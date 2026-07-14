package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.HttpException;
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

        if (newPassword == null) throw new HttpException(400, "password is required");

        String passError = ValidationUtils.validatePassword(newPassword);
        if (passError != null) throw new HttpException(400, passError);

        String username = (String) exchange.getAttribute("username");

        if (!SqlUtils.updatePassword(username, newPassword)) {
            log.warn("Password update failed: {} not found", username);
            throw new HttpException(404, "User not found");
        }

        TokenBlacklist.invalidate(token);
        ResponseUtils.send(exchange, 200, "{\"message\": \"Password updated\"}");
        log.info("Password updated for {}", username);
    }
}
