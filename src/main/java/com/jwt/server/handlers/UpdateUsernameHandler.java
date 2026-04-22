package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class UpdateUsernameHandler implements HttpHandler {
    
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
        
        if (exchange.getRequestMethod().equals("DELETE")) {
            String username = JwtUtils.extractUsername(token);
            try {
                SqlUtils.updateUsername(username, newUsername);
                String response = "{\"message\": \"User " + username + " was updated to " + newUsername + "\"}";
                ResponseUtils.send(exchange, 200, response);
            } catch (Exception e) {
                ResponseUtils.sendError(exchange, 500, "Internal Server Error");
            }
        }
    }
}