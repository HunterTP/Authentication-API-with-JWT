package com.jwt.server.handlers;

import java.io.IOException;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.JwtUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class DeleteUserHandler implements HttpHandler {
    
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
        
        if (exchange.getRequestMethod().equals("DELETE")) {
            String username = JwtUtils.extractUsername(token);
            try {
                SqlUtils.deleteUser(username);
                String response = "{\"message\": \"User " + username + " was deleted\"}";
                ResponseUtils.send(exchange, 200, response);
            } catch (Exception e) {
                ResponseUtils.sendError(exchange, 500, "Internal Server Error");
            }
        }
    }
}