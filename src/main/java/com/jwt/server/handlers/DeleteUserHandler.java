package com.jwt.server.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.jwt.server.utils.TokenBlacklist;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeleteUserHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(DeleteUserHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsUtils.addCorsHeaders(exchange);
        if (CorsUtils.handleOptionsRequest(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equals("DELETE")) {
            ResponseUtils.sendError(exchange, 405, "Only DELETE method is allowed");
            return;
        }

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ResponseUtils.sendError(exchange, 401, "No Token provided");
            return;
        }

        String token = authHeader.substring(7);
        String username;
        try {
            username = JwtUtils.extractUsername(token);
        } catch (Exception e) {
            ResponseUtils.sendError(exchange, 401, "Invalid token");
            return;
        }

        try {
            boolean deleted = SqlUtils.deleteUser(username);
            if (!deleted) {
                ResponseUtils.sendError(exchange, 404, "User not found");
                log.warn("Delete failed: {} not found", username);
                return;
            }
            TokenBlacklist.invalidate(token);
            String response = "{\"message\": \"User " + username + " was deleted\"}";
            ResponseUtils.send(exchange, 200, response);
            log.info("User deleted: {}", username);
        } catch (Exception e) {
            log.error("Delete failed for {}: {}", username, e.getMessage());
            ResponseUtils.sendError(exchange, 500, "Internal Server Error");
        }
    }
}
