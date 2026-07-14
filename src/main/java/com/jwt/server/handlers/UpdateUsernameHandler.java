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

public class UpdateUsernameHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(UpdateUsernameHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = (String) exchange.getAttribute("token");

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String newUsername = JsonUtils.extractValue(body, "username");

        if (newUsername == null) throw new HttpException(400, "username is required");

        String userError = ValidationUtils.validateUsername(newUsername);
        if (userError != null) throw new HttpException(400, userError);

        String oldUsername = (String) exchange.getAttribute("username");

        if (!SqlUtils.updateUsername(oldUsername, newUsername)) {
            log.warn("Username update failed: {} not found", oldUsername);
            throw new HttpException(404, "User not found");
        }

        TokenBlacklist.invalidate(token);
        ResponseUtils.send(exchange, 200, "{\"message\": \"Username updated\"}");
        log.info("Username changed from {} to {}", oldUsername, newUsername);
    }
}
