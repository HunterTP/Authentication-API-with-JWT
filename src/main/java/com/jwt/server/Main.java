package com.jwt.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.config.Config;
import com.jwt.server.http.HttpsUtils;
import com.jwt.server.persistence.SqlUtils;
import com.jwt.server.persistence.TokenBlacklist;
import com.jwt.server.security.AccountLocker;
import com.jwt.server.security.RateLimiter;
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

            if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof ch.qos.logback.classic.LoggerContext context) {
                context.stop();
            }
        }));

        if (!SqlUtils.testConnection()) {
            log.error("Database connection failed. Exiting.");
            return;
        }

        TokenBlacklist.init();

        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cleanup");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            try {
                TokenBlacklist.cleanup();
                RateLimiter.cleanupAll();
                AccountLocker.cleanupAll();
            } catch (Exception e) {
                log.warn("Cleanup task failed: {}", e.getMessage());
            }
        }, 15, 15, TimeUnit.MINUTES);
        log.info("Cleanup scheduled every 15 minutes");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleaner.shutdown();
        }));

        try {
            server = HttpsUtils.createHttpsServer();
            if (server == null) {
                log.error("HTTPS server could not be started. Exiting.");
                return;
            }
            log.info("Server is ready on https://localhost:{}", Config.PORT);
        } catch (Exception e) {
            log.error("Fatal error: {}", e.getMessage());
        }
    }
}
