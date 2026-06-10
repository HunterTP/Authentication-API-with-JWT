package com.jwt.server.utils;

import org.mindrot.jbcrypt.BCrypt;

public class JbcryptUtils {

    private static final int WORKLOAD = 15;
    private static final String PEPPER = System.getenv("BCRYPT_PEPPER");

    public String[] Hash(String password) {
        String[] hash = new String[2];
        hash[0] = BCrypt.gensalt(WORKLOAD);
        hash[1] = BCrypt.hashpw(password, hash[0] + PEPPER);
        return hash;
    }

    public String HashPassword(String password, String username) {
        String salt = SqlUtils.getSalt(username);
        String hashedpassword = BCrypt.hashpw(password, salt);
        return hashedpassword;
    }
}