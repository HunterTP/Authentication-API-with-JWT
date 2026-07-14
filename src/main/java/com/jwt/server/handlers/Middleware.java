package com.jwt.server.handlers;

import com.jwt.server.utils.CorsUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Middleware {

    public static HttpHandler wrap(HttpHandler handler) {
        return (HttpExchange exchange) -> {
            CorsUtils.addCorsHeaders(exchange);
            if (CorsUtils.handleOptionsRequest(exchange)) {
                return;
            }
            handler.handle(exchange);
        };
    }
}
