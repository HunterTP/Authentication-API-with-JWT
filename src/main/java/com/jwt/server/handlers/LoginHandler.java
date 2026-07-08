package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.AccountLocker;
import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.RateLimiter;
import com.jwt.server.utils.RequestUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);
    private static final RateLimiter rateLimiter = new RateLimiter();
    private static final AccountLocker accountLocker = new AccountLocker();

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

        if (!RequestUtils.isJsonContentType(exchange)) {
            ResponseUtils.sendError(exchange, 415, "Content-Type must be application/json");
            return;
        }

        String clientIp = exchange.getRemoteAddress() != null
            ? exchange.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        if (!rateLimiter.isAllowed(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
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

        if (accountLocker.isLocked(username)) {
            log.warn("Account locked for {}", username);
            ResponseUtils.sendError(exchange, 429, "Account is temporarily locked. Try again later.");
            return;
        }

        if (SqlUtils.checkUserCredentials(username, password)) {
            accountLocker.reset(username);
            String token = JwtUtils.generateToken(username);
            String response = "{\"token\":\"" + token + "\", \"message\":\"Login successful\"}";
            ResponseUtils.send(exchange, 200, response);
            log.info("Login successful: {}", username);
        } else {
            accountLocker.recordFailedAttempt(username);
            log.warn("Login failed for {}", username);
            ResponseUtils.sendError(exchange, 401, "username or password is incorrect");
        }
    }
}
