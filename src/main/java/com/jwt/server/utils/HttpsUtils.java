package com.jwt.server.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.jwt.server.handlers.DeleteUserHandler;
import com.jwt.server.handlers.HealthHandler;
import com.jwt.server.handlers.LoginHandler;
import com.jwt.server.handlers.RegisterHandler;
import com.jwt.server.handlers.UpdatePasswordHandler;
import com.jwt.server.handlers.UpdateUsernameHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

public class HttpsUtils {

    private static final String STROREPASS = System.getenv("storepass");
    private static final String KEYPASS = System.getenv("keypass");

    public static HttpsServer createHttpsServer() throws Exception {
        
        HttpsServer server = HttpsServer.create(new InetSocketAddress(8443), 0);
        
        try {
                // Load Key (keystore.jks)
                char[] storepass = STROREPASS.toCharArray();
                KeyStore ks = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream("keystore.jks")) {
                    ks.load(fis, storepass);
                }

                // Initialise KeyManager
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, KEYPASS.toCharArray());

                // Create SSLContext
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, null);

                // Connect SSLContext with HttpsServer
                server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    @Override
                    public void configure(HttpsParameters params) {
                        SSLContext c = getSSLContext();
                        params.setSSLParameters(c.getDefaultSSLParameters());
                    }
                });

                // Register Endpoints
                server.createContext("/auth/register", new RegisterHandler());
                server.createContext("/auth/login", new LoginHandler());
                server.createContext("/auth/user/delete", new DeleteUserHandler());
                server.createContext("/auth/user/password", new UpdatePasswordHandler());
                server.createContext("/auth/user/username", new UpdateUsernameHandler());
                server.createContext("/api/health", new HealthHandler());

                server.start();
            } catch (java.security.KeyStoreException | java.security.NoSuchAlgorithmException | java.security.UnrecoverableKeyException | java.security.KeyManagementException | java.io.IOException e) {
                System.err.println("Error with the SSL Initialization: " + e.getMessage());
                server = null;
            }
            return server;
    }
}
