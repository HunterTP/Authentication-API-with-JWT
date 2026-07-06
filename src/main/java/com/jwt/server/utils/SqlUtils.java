package com.jwt.server.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;

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
        
        System.out.println("Database Config loaded: " + DB_URL);
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

    // Registers a new user
    public static void registerUser(HttpExchange exchange, String username, String password) throws Exception {
        String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String[] hash = new JbcryptUtils().Hash(password);

            pstmt.setString(1, username);
            pstmt.setString(2, hash[1]);
            pstmt.setString(3, hash[0]);
            int rows = pstmt.executeUpdate();

            if (rows == 0) {
                ResponseUtils.sendError(exchange, 500, "User could not be created");
                return;
            }

            String response = "{\"message\": \"User " + username + " was created\"}";
            ResponseUtils.send(exchange, 201, response);
            
        } catch (Exception e) {
            if (e.getMessage().contains("Duplicate entry")) {
                ResponseUtils.sendError(exchange, 409, "Username already exists");
            } else {
                ResponseUtils.sendError(exchange, 500, "Database error: " + e.getMessage());
            }
        }
    }

    // Deletes a user
    public static boolean deleteUser(String username) throws Exception {
        String sql = "DELETE FROM users WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            throw new Exception("Database error: " + e.getMessage());
        }
    }

    // Updates a user's username
    public static boolean updateUsername(String oldUsername, String newUsername) throws Exception {
        String sql = "UPDATE users SET username = ? WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, newUsername);
            pstmt.setString(2, oldUsername);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            throw new Exception("Database error: " + e.getMessage());
        }
    }

    public static boolean updatePassword(String username, String newPassword) throws Exception {
        String sql = "UPDATE users SET password = ?, salt = ? WHERE username = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String[] hash = new JbcryptUtils().Hash(newPassword);

            pstmt.setString(1, hash[1]);
            pstmt.setString(2, hash[0]);
            pstmt.setString(3, username);
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            throw new Exception("Database error: " + e.getMessage());
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
}