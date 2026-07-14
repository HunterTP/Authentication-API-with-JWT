package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.AccountLocker;
import com.jwt.server.utils.HttpException;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.RateLimiter;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.ValidationUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginHandler.class);
    private static final RateLimiter rateLimiter = RateLimiter.global();
    private static final AccountLocker accountLocker = AccountLocker.global();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String clientIp = exchange.getRemoteAddress() != null
            ? exchange.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";

        if (!rateLimiter.isAllowed(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            throw new HttpException(429, "Too many requests. Please try again later.");
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String username = JsonUtils.extractValue(body, "username");
        String password = JsonUtils.extractValue(body, "password");

        if (username == null || password == null) {
            throw new HttpException(400, "username and password are required");
        }

        String userError = ValidationUtils.validateUsername(username);
        if (userError != null) throw new HttpException(400, userError);

        String passError = ValidationUtils.validatePassword(password);
        if (passError != null) throw new HttpException(400, passError);

        if (accountLocker.isLocked(username)) {
            log.warn("Account locked for {}", username);
            throw new HttpException(429, "Account is temporarily locked. Try again later.");
        }

        if (!SqlUtils.checkUserCredentials(username, password)) {
            accountLocker.recordFailedAttempt(username);
            log.warn("Login failed for {}", username);
            throw new HttpException(401, "username or password is incorrect");
        }

        accountLocker.reset(username);

        String token = JwtUtils.generateToken(username);
        String response = "{\"token\":\"" + token + "\", \"message\":\"Login successful\"}";
        ResponseUtils.send(exchange, 200, response);
        log.info("Login successful: {}", username);
    }
}
