package com.jwt.server.handler;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.exception.HttpException;
import com.jwt.server.http.ResponseUtils;
import com.jwt.server.persistence.SqlUtils;
import com.jwt.server.persistence.TokenBlacklist;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeleteUserHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = (String) exchange.getAttribute("token");
        String username = (String) exchange.getAttribute("username");

        if (!SqlUtils.deleteUser(username)) {
            log.warn("Delete failed: {} not found", username);
            throw new HttpException(404, "User not found");
        }

        TokenBlacklist.invalidate(token);
        ResponseUtils.send(exchange, 200, "{\"message\": \"User " + username + " was deleted\"}");
        log.info("User deleted: {}", username);
    }
}
