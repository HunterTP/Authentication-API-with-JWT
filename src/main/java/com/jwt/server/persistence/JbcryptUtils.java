package com.jwt.server.persistence;

import org.mindrot.jbcrypt.BCrypt;

import com.jwt.server.config.Config;
import com.jwt.server.exception.HttpException;

public final class JbcryptUtils {

    private static final int WORKLOAD = Config.BCRYPT_WORKLOAD;
    private static final String PEPPER = Config.BCRYPT_PEPPER;

    private JbcryptUtils() {}

    public static String[] hashPasswordPair(String password) {
        String salt = BCrypt.gensalt(WORKLOAD);
        String hash = BCrypt.hashpw((PEPPER != null ? PEPPER : "") + password, salt);
        return new String[]{salt, hash};
    }

    public static String hashPassword(String password, String username) throws HttpException {
        String salt = SqlUtils.getSalt(username);
        if (salt == null) return null;
        return BCrypt.hashpw((PEPPER != null ? PEPPER : "") + password, salt);
    }
}
