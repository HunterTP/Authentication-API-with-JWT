package com.jwt.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.jwt.server.utils.HttpException;

public class SqlUtils {
    // Database connection parameters
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        // Read from environment variables
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        
        // Development fallback
        if (url == null || url.isEmpty()) {
            url = "jdbc:mysql://localhost:3306/authdb";
        }
        if (user == null || user.isEmpty()) {
            user = "root";
        }
        if (password == null || password.isEmpty()) {
            password = "root";
        }
        
        DB_URL = url;
        DB_USER = user;
        DB_PASSWORD = password;
        
        String logUrl = DB_URL.replaceAll("(?<=://)[^:]+:[^@]+@", "***:***@");
        System.out.println("Database config loaded: " + logUrl);
    }

    // Returns a new database connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Tests the database connection
    public static boolean testConnection() {
        try (Connection conn = SqlUtils.getConnection()) {
            System.out.println("Database connection successful!");
            conn.close();
            return true;
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.out.println("ERROR: Cannot establish database connection!");
            return false;
        }
    }

    public static boolean registerUser(String username, String password) throws HttpException {
        String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String[] hash = new JbcryptUtils().Hash(password);

            pstmt.setString(1, username);
            pstmt.setString(2, hash[1]);
            pstmt.setString(3, hash[0]);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new HttpException(409, "Username already exists");
            }
            throw new HttpException(500, "Internal Server Error");
        }
    }

    // Deletes a user
    public static boolean deleteUser(String username) throws HttpException {
        String sql = "DELETE FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            throw new HttpException(500, "Internal Server Error");
        }
    }

    public static boolean updateUsername(String oldUsername, String newUsername) throws HttpException {
        String sql = "UPDATE users SET username = ? WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newUsername);
            pstmt.setString(2, oldUsername);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new HttpException(409, "Username already exists");
            }
            throw new HttpException(500, "Internal Server Error");
        }
    }

    public static boolean updatePassword(String username, String newPassword) throws HttpException {
        String sql = "UPDATE users SET password = ?, salt = ? WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String[] hash = new JbcryptUtils().Hash(newPassword);

            pstmt.setString(1, hash[1]);
            pstmt.setString(2, hash[0]);
            pstmt.setString(3, username);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            throw new HttpException(500, "Internal Server Error");
        }
    }

    // Verify user credentials
    public static boolean checkUserCredentials(String username, String password) {
        String hashedPassword = new JbcryptUtils().HashPassword(password, username);
        if (hashedPassword == null) return false;

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
            
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }

    public static String getSalt (String username) {
        String sql = "SELECT salt FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("salt");
            }
            return null;
            
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            return null;
        }
    }

    public static Map<String, Long> loadBlacklistedTokens() {
        Map<String, Long> result = new java.util.HashMap<>();
        String sql = "SELECT token_hash, expires_at FROM token_blacklist WHERE expires_at > ?";
        long now = System.currentTimeMillis();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, now);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("token_hash"), rs.getLong("expires_at"));
            }
        } catch (Exception e) {
            System.err.println("Failed to load blacklisted tokens: " + e.getMessage());
        }
        return result;
    }

    public static void persistBlacklistedToken(String token, long expiresAt) {
        String sql = "INSERT INTO token_blacklist (token_hash, expires_at) VALUES (?, ?) ON DUPLICATE KEY UPDATE expires_at = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.setLong(2, expiresAt);
            pstmt.setLong(3, expiresAt);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Failed to persist blacklisted token: " + e.getMessage());
        }
    }

    public static void removeExpiredBlacklistedTokens() {
        String sql = "DELETE FROM token_blacklist WHERE expires_at <= ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            int removed = pstmt.executeUpdate();
            if (removed > 0) {
                System.out.println("Cleaned up " + removed + " expired blacklisted tokens");
            }
        } catch (Exception e) {
            System.err.println("Failed to cleanup expired tokens: " + e.getMessage());
        }
    }
}