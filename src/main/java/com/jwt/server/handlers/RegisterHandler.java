package com.jwt.server.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JsonUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegisterHandler implements HttpHandler {
    
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
        
        try {
            SqlUtils.registerUser(exchange, username, password);
        } catch (Exception e) {
            ResponseUtils.sendError(exchange, 500, "Error occurred while registering the user");
        }
    }
}