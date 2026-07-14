package com.jwt.server.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jwt.server.handlers.DeleteUserHandler;
import com.jwt.server.handlers.HealthHandler;
import com.jwt.server.handlers.LoginHandler;
import com.jwt.server.handlers.Middleware;
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

            String keystorePath = (Config.KEYSTORE_PATH != null && !Config.KEYSTORE_PATH.isEmpty())
                ? Config.KEYSTORE_PATH : "keystore.jks";

            File ksFile = new File(keystorePath);
            if (!ksFile.exists()) {
                log.info("Keystore not found, generating self-signed certificate...");
                ProcessBuilder pb = new ProcessBuilder(
                    "keytool", "-genkeypair", "-alias", "selfsigned",
                    "-keyalg", "RSA", "-keysize", "2048",
                    "-validity", "3650", "-storetype", "PKCS12",
                    "-keystore", keystorePath,
                    "-storepass", storepass, "-keypass", keypass,
                    "-noprompt",
                    "-dname", "CN=localhost, OU=Dev, O=Authentication-API, L=Unknown, ST=Unknown, C=Unknown"
                );
                pb.inheritIO();
                Process p = pb.start();
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    log.error("keytool failed with exit code {}", exitCode);
                    return null;
                }
                log.info("Self-signed keystore generated: {}", keystorePath);
            }

            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
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

            RegisterHandler registerHandler = new RegisterHandler();
            LoginHandler loginHandler = new LoginHandler();
            DeleteUserHandler deleteUserHandler = new DeleteUserHandler();
            UpdatePasswordHandler updatePasswordHandler = new UpdatePasswordHandler();
            UpdateUsernameHandler updateUsernameHandler = new UpdateUsernameHandler();
            HealthHandler healthHandler = new HealthHandler();

            server.createContext("/v1/auth/register", Middleware.wrap(registerHandler, "POST"));
            server.createContext("/v1/auth/login", Middleware.wrap(loginHandler, "POST"));
            server.createContext("/v1/auth/user/delete", Middleware.wrapProtected(deleteUserHandler, "DELETE"));
            server.createContext("/v1/auth/user/password", Middleware.wrapProtected(updatePasswordHandler, "PUT"));
            server.createContext("/v1/auth/user/username", Middleware.wrapProtected(updateUsernameHandler, "PUT"));
            server.createContext("/v1/api/health", Middleware.wrap(healthHandler, "GET"));

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            log.info("HTTPS server listening on port {}", Config.PORT);

        } catch (IOException | InterruptedException | GeneralSecurityException e) {
            log.error("SSL initialization failed: {}", e.getMessage());
            server = null;
        }
        return server;
    }
}
