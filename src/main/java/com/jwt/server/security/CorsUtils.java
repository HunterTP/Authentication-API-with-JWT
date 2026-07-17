package com.jwt.server.security;

import java.io.IOException;

import com.jwt.server.config.Config;
import com.sun.net.httpserver.HttpExchange;

public class CorsUtils {

    public static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", Config.CORS_ORIGIN);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", 
            "GET, POST, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", 
            "Content-Type, Authorization, Accept, X-Requested-With");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "3600");
    }
    
    public static boolean handleOptionsRequest(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }
}
