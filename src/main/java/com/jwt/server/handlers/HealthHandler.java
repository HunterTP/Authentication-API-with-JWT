package com.jwt.server.handlers;

import java.io.IOException;

import com.jwt.server.utils.ResponseUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HealthHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Nur GET erlauben
        if (!exchange.getRequestMethod().equals("GET")) {
            ResponseUtils.sendError(exchange, 405, "Only GET allowed");
            return;
        }
        
        // Einfache Status-Antwort
        String response = String.format(
            "{\"status\":\"ok\",\"timestamp\":%d}",
            System.currentTimeMillis()
        );
        
        ResponseUtils.send(exchange, 200, response);
    }
}