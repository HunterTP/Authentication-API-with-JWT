package com.jwt.server.handlers;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.ResponseUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Middleware {

    public static HttpHandler wrap(HttpHandler handler, String... allowedMethods) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) {
                return;
            }

            String method = exchange.getRequestMethod();
            boolean allowed = false;
            for (String m : allowedMethods) {
                if (m.equalsIgnoreCase(method)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                ResponseUtils.sendError(exchange, 405, "Only " + String.join(", ", allowedMethods) + " allowed");
                return;
            }

            handler.handle(exchange);
        };
    }
}
