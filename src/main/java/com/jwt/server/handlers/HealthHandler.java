package com.jwt.server.handlers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.CorsUtils;
import com.jwt.server.utils.ResponseUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HealthHandler implements HttpHandler {

    private static final Logger log = LoggerFactory.getLogger(HealthHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CorsUtils.addCorsHeaders(exchange);
        if (CorsUtils.handleOptionsRequest(exchange)) {
            return;
        }

        if (!exchange.getRequestMethod().equals("GET")) {
            ResponseUtils.sendError(exchange, 405, "Only GET allowed");
            return;
        }

        boolean dbOk = SqlUtils.testConnection();
        String status = dbOk ? "ok" : "degraded";
        int statusCode = dbOk ? 200 : 503;

        String response = String.format(
            "{\"status\":\"%s\",\"database\":%b,\"timestamp\":%d}",
            status, dbOk, System.currentTimeMillis()
        );

        ResponseUtils.send(exchange, statusCode, response);

        if (!dbOk) {
            log.warn("Healthcheck: database unreachable");
        }
    }
}
