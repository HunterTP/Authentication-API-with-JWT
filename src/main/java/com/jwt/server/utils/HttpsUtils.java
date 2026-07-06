package com.jwt.server.utils;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(HttpsUtils.class);

    public static HttpsServer createHttpsServer() throws Exception {

        HttpsServer server = HttpsServer.create(new InetSocketAddress(Config.PORT), 0);

        try {
            String storepass = Config.KEYSTORE_PASS;
            String keypass = Config.KEY_PASS;

            if (storepass == null || keypass == null) {
                log.error("KEYSTORE_PASS and KEY_PASS must be set");
                return null;
            }

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            String keystorePath = (Config.KEYSTORE_PATH != null && !Config.KEYSTORE_PATH.isEmpty())
                ? Config.KEYSTORE_PATH : "keystore.jks";
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                ks.load(fis, storepass.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, keypass.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                @Override
                public void configure(HttpsParameters params) {
                    SSLContext c = getSSLContext();
                    params.setSSLParameters(c.getDefaultSSLParameters());
                }
            });

            String[] paths = {
                "/auth/register",       "/v1/auth/register",
                "/auth/login",           "/v1/auth/login",
                "/auth/user/delete",     "/v1/auth/user/delete",
                "/auth/user/password",   "/v1/auth/user/password",
                "/auth/user/username",   "/v1/auth/user/username",
                "/api/health",           "/v1/api/health"
            };

            RegisterHandler registerHandler = new RegisterHandler();
            LoginHandler loginHandler = new LoginHandler();
            DeleteUserHandler deleteUserHandler = new DeleteUserHandler();
            UpdatePasswordHandler updatePasswordHandler = new UpdatePasswordHandler();
            UpdateUsernameHandler updateUsernameHandler = new UpdateUsernameHandler();
            HealthHandler healthHandler = new HealthHandler();

            for (int i = 0; i < paths.length; i++) {
                switch (paths[i]) {
                    case "/auth/register":
                    case "/v1/auth/register":
                        server.createContext(paths[i], registerHandler);
                        break;
                    case "/auth/login":
                    case "/v1/auth/login":
                        server.createContext(paths[i], loginHandler);
                        break;
                    case "/auth/user/delete":
                    case "/v1/auth/user/delete":
                        server.createContext(paths[i], deleteUserHandler);
                        break;
                    case "/auth/user/password":
                    case "/v1/auth/user/password":
                        server.createContext(paths[i], updatePasswordHandler);
                        break;
                    case "/auth/user/username":
                    case "/v1/auth/user/username":
                        server.createContext(paths[i], updateUsernameHandler);
                        break;
                    case "/api/health":
                    case "/v1/api/health":
                        server.createContext(paths[i], healthHandler);
                        break;
                }
            }

            server.start();
            log.info("HTTPS server listening on port {}", Config.PORT);

        } catch (Exception e) {
            log.error("SSL initialization failed: {}", e.getMessage());
            server = null;
        }
        return server;
    }
}
