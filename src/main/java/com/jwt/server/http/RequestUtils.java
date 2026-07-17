package com.jwt.server.http;

import com.sun.net.httpserver.HttpExchange;

public final class RequestUtils {

    private static final String APPLICATION_JSON = "application/json";

    private RequestUtils() {}

    public static boolean hasContentType(HttpExchange exchange, String expected) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null) return false;
        int semi = contentType.indexOf(';');
        if (semi >= 0) contentType = contentType.substring(0, semi).trim();
        return expected.equalsIgnoreCase(contentType);
    }

    public static boolean isJsonContentType(HttpExchange exchange) {
        return hasContentType(exchange, APPLICATION_JSON);
    }
}
