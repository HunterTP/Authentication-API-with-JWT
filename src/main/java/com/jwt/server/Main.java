package com.jwt.server;

import java.io.IOException;

import com.jwt.server.utils.HttpsUtils;
import com.jwt.server.utils.SqlUtils;
import com.sun.net.httpserver.HttpsServer;

public class Main {
    
    public static void main(String[] args) throws IOException {
        // Test Database connection before starting the server
        if (!SqlUtils.testConnection()) {
            return;
        }
        
        // Start HTTPS server
        try {
            HttpsServer server = HttpsUtils.createHttpsServer();
            if (server == null) {
                System.err.println("ERROR: HTTPS-Server could not be started.");
                return;
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return;
        }
        
        System.out.println("\nHTTPS-Server is running on https://localhost:8443");
        System.out.println("  POST /auth/register      - Create new user");
        System.out.println("  POST /auth/login         - Login (returns token)");
        System.out.println("  DELETE /auth/user/delete - Delete user (requires token)");
        System.out.println("  PUT /auth/user/password  - Update user password (requires token)");
        System.out.println("  PUT /auth/user/username  - Update user username (requires token)");
    }
}