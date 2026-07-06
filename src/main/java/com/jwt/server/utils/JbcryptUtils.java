package com.jwt.server.utils;

import org.mindrot.jbcrypt.BCrypt;

public class JbcryptUtils {

    private static final int WORKLOAD = Config.BCRYPT_WORKLOAD;
    private static final String PEPPER = Config.BCRYPT_PEPPER;

    public String[] Hash(String password) {
        String[] hash = new String[2];
        hash[0] = BCrypt.gensalt(WORKLOAD);
        hash[1] = BCrypt.hashpw((PEPPER != null ? PEPPER : "") + password, hash[0]);
        return hash;
    }

    public String HashPassword(String password, String username) {
        String salt = SqlUtils.getSalt(username);
        if (salt == null) return null;
        String hashedpassword = BCrypt.hashpw((PEPPER != null ? PEPPER : "") + password, salt);
        return hashedpassword;
    }
}
