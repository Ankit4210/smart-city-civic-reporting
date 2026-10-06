package com.smartcity.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection utility for the Smart City Civic Issue Reporting System.
 */
public class DBUtil {

    // Default values — overridden by web.xml context-params at runtime
    private static String DB_URL = "jdbc:mysql://localhost:3306/smart_city_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";
    private static String DB_USER = "root";
    private static String DB_PASSWORD = "";

    /**
     * Initialize DB config from web.xml context-params.
     * Called once by AppContextListener during application startup.
     */
    public static void init(String url, String user, String password) {
        String configuredUrl = System.getenv("SMARTCITY_DB_URL");
        String configuredUser = System.getenv("SMARTCITY_DB_USER");
        String configuredPassword = System.getenv("SMARTCITY_DB_PASSWORD");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            DB_URL = configuredUrl;
        } else if (url != null && !url.isBlank()) {
            DB_URL = url;
        }
        if (configuredUser != null && !configuredUser.isBlank()) {
            DB_USER = configuredUser;
        } else if (user != null && !user.isBlank()) {
            DB_USER = user;
        }
        if (configuredPassword != null) {
            DB_PASSWORD = configuredPassword;
        } else if (password != null) {
            DB_PASSWORD = password;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver is unavailable.", e);
        }
        System.out.println("[DBUtil] Database configured: " + DB_URL);
    }

    /**
     * Get a new database connection.
     * Each servlet call should get a connection, use it, and close it in finally block.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Safely close a connection.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
