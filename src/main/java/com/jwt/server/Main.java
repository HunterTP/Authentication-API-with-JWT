package com.jwt.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.utils.HttpsUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpsServer;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static HttpsServer server;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            if (server != null) {
                server.stop(2);
            }
            log.info("Server stopped");
        }));

        if (!SqlUtils.testConnection()) {
            log.error("Database connection failed. Exiting.");
            return;
        }

        try {
            server = HttpsUtils.createHttpsServer();
            if (server == null) {
                log.error("HTTPS server could not be started. Exiting.");
                return;
            }
            log.info("Server is ready on https://localhost:{}", com.jwt.server.utils.Config.PORT);
        } catch (Exception e) {
            log.error("Fatal error: {}", e.getMessage());
        }
    }
}
