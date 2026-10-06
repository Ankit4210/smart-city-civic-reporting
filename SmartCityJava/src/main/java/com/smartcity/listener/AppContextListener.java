package com.smartcity.listener;

import com.smartcity.util.DBUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Application Context Listener
 * Initializes the database connection and creates tables on application startup.
 */
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("====================================================");
        System.out.println("  SAFE & SMART CITY - CIVIC ISSUE REPORTING SYSTEM  ");
        System.out.println("  Initializing Application Context...               ");
        System.out.println("====================================================");

        ServletContext ctx = sce.getServletContext();

        // Read DB config from web.xml context-params
        String dbUrl = ctx.getInitParameter("DB_URL");
        String dbUser = ctx.getInitParameter("DB_USER");
        String dbPassword = ctx.getInitParameter("DB_PASSWORD");

        // Initialize DBUtil with config
        DBUtil.init(dbUrl, dbUser, dbPassword);

        // Create tables and seed data
        try {
            initDatabase();
            System.out.println("[AppContextListener] Database tables initialized successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("Civic reporting database initialization failed.", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[AppContextListener] Application shutting down...");
    }

    /**
     * Create all required tables and seed initial data.
     */
    private void initDatabase() throws Exception {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();

            // Create Database (if not exists)
            // Note: The database should already exist; this is for MySQL
            // CREATE DATABASE is handled externally or in the SQL script

            // 1. Users Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100) NOT NULL," +
                "  email VARCHAR(150) NOT NULL UNIQUE," +
                "  password VARCHAR(255) NOT NULL," +
                "  phone VARCHAR(20) DEFAULT NULL," +
                "  role ENUM('citizen', 'admin', 'officer') DEFAULT 'citizen'," +
                "  civic_points INT DEFAULT 50," +
                "  ward VARCHAR(100) DEFAULT 'Ward 12 - Central'," +
                "  avatar VARCHAR(255) DEFAULT 'default-avatar.png'," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_users_email (email)," +
                "  INDEX idx_users_role (role)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 2. Categories Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS categories (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100) NOT NULL," +
                "  code VARCHAR(50) NOT NULL UNIQUE," +
                "  icon VARCHAR(50) NOT NULL," +
                "  department VARCHAR(100) NOT NULL," +
                "  sla_hours INT DEFAULT 48," +
                "  description TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 3. Reports Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS reports (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  tracking_id VARCHAR(50) NOT NULL UNIQUE," +
                "  user_id INT NOT NULL," +
                "  category_id INT NOT NULL," +
                "  title VARCHAR(200) NOT NULL," +
                "  description TEXT NOT NULL," +
                "  landmark VARCHAR(200) DEFAULT NULL," +
                "  address VARCHAR(255) DEFAULT NULL," +
                "  ward VARCHAR(100) DEFAULT 'Ward 12 - Central'," +
                "  latitude DECIMAL(10, 7) NOT NULL," +
                "  longitude DECIMAL(10, 7) NOT NULL," +
                "  before_photo VARCHAR(255) DEFAULT NULL," +
                "  priority ENUM('Low', 'Medium', 'High', 'Critical') DEFAULT 'Medium'," +
                "  status ENUM('Pending', 'In Progress', 'Resolved', 'Rejected') DEFAULT 'Pending'," +
                "  upvotes INT DEFAULT 1," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT," +
                "  INDEX idx_reports_tracking (tracking_id)," +
                "  INDEX idx_reports_status (status)," +
                "  INDEX idx_reports_ward (ward)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 4. Resolutions Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS resolutions (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  report_id INT NOT NULL UNIQUE," +
                "  admin_id INT NOT NULL," +
                "  after_photo VARCHAR(255) NOT NULL," +
                "  remarks TEXT NOT NULL," +
                "  action_taken VARCHAR(200) DEFAULT 'Site rectified by municipal crew'," +
                "  resolved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (admin_id) REFERENCES users(id) ON DELETE RESTRICT" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 5. Upvotes Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS upvotes (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  report_id INT NOT NULL," +
                "  user_id INT NOT NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE KEY uk_report_user_upvote (report_id, user_id)," +
                "  FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 6. Feedback Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS feedback (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  report_id INT NOT NULL UNIQUE," +
                "  user_id INT NOT NULL," +
                "  rating INT NOT NULL," +
                "  is_satisfied TINYINT(1) NOT NULL DEFAULT 1," +
                "  comments TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 7. Notifications Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notifications (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  report_id INT DEFAULT NULL," +
                "  title VARCHAR(150) NOT NULL," +
                "  message TEXT NOT NULL," +
                "  type VARCHAR(50) DEFAULT 'status_update'," +
                "  is_read TINYINT(1) DEFAULT 0," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            // 8. Activity Logs Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS activity_logs (" +
                "  id INT AUTO_INCREMENT PRIMARY KEY," +
                "  report_id INT NOT NULL," +
                "  actor_name VARCHAR(100) NOT NULL," +
                "  action VARCHAR(100) NOT NULL," +
                "  details TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );

            System.out.println("[DB] All 8 tables created/verified.");

            // Seed categories if empty
            var rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM categories");
            rs.next();
            if (rs.getInt("cnt") == 0) {
                seedCategories(stmt);
            }
            rs.close();

            // Seed admin user if no users exist
            rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM users");
            rs.next();
            if (rs.getInt("cnt") == 0) {
                if ("production".equalsIgnoreCase(System.getenv("SMARTCITY_ENV"))) {
                    seedProductionAdmin(conn);
                } else {
                    seedUsers(conn);
                }
            }
            rs.close();
            repairLegacyDemoPasswords(conn);

        } finally {
            if (stmt != null) stmt.close();
            DBUtil.closeConnection(conn);
        }
    }

    private void seedCategories(Statement stmt) throws Exception {
        System.out.println("[DB] Seeding civic issue categories...");
        String[] inserts = {
            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Garbage & Waste Accumulation', 'GARBAGE', 'fa-trash-can', 'Solid Waste Management', 24, 'Overflowing bins, illegal garbage dumps, bio-waste')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Potholes & Damaged Roads', 'ROADS', 'fa-road-circle-exclamation', 'Roads & Infrastructure', 72, 'Road craters, broken tarmac, missing manhole covers')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Streetlight Failure & Dark Spots', 'LIGHTING', 'fa-lightbulb', 'Electrical Engineering', 24, 'Non-functional lamps, hanging wires, low lighting')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Water Leakage & Pipeline Burst', 'WATER', 'fa-faucet-drip', 'Water Supply & Sewerage', 12, 'Drinking water pipeline bursts, contaminated supply')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Drainage & Sewage Overflow', 'DRAINAGE', 'fa-water', 'Public Health Engineering', 24, 'Blocked storm drains, open sewage overflow')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Public Littering & Urination', 'HYGIENE', 'fa-hand-sparkles', 'Sanitation & Health', 48, 'Public hygiene violations, unsanitary public spaces')",

            "INSERT INTO categories (name, code, icon, department, sla_hours, description) VALUES " +
            "('Broken Footpath & Encroachment', 'FOOTPATH', 'fa-person-walking-dashed-line-arrow-right', 'Town Planning & Enforcement', 96, 'Destroyed pedestrian walkways, unauthorized stalls')"
        };

        for (String sql : inserts) {
            stmt.executeUpdate(sql);
        }
        System.out.println("[DB] 7 civic categories seeded successfully.");
    }

    private void seedUsers(Connection conn) throws Exception {
        System.out.println("[DB] Seeding admin and citizen accounts...");
        String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt(10));
        String citizenPassword = org.mindrot.jbcrypt.BCrypt.hashpw("citizen123", org.mindrot.jbcrypt.BCrypt.gensalt(10));

        var ps = conn.prepareStatement(
            "INSERT INTO users (name, email, password, phone, role, civic_points, ward) VALUES (?, ?, ?, ?, ?, ?, ?)"
        );

        // Admin
        ps.setString(1, "Demo Municipal Admin");
        ps.setString(2, "admin@example.test");
        ps.setString(3, hashedPassword);
        ps.setString(4, "0000000001");
        ps.setString(5, "admin");
        ps.setInt(6, 500);
        ps.setString(7, "Headquarters");
        ps.executeUpdate();

        // Citizen 1
        ps.setString(1, "Demo Citizen One");
        ps.setString(2, "citizen.one@example.test");
        ps.setString(3, citizenPassword);
        ps.setString(4, "0000000003");
        ps.setString(5, "citizen");
        ps.setInt(6, 220);
        ps.setString(7, "Ward 12 - Central");
        ps.executeUpdate();

        // Citizen 2
        ps.setString(1, "Demo Citizen Two");
        ps.setString(2, "citizen.two@example.test");
        ps.setString(3, citizenPassword);
        ps.setString(4, "0000000004");
        ps.setString(5, "citizen");
        ps.setInt(6, 180);
        ps.setString(7, "Ward 7 - North");
        ps.executeUpdate();

        ps.close();
        System.out.println("[DB] Demo admin and citizen accounts seeded (see project README).");
    }

    private void seedProductionAdmin(Connection conn) throws Exception {
        String email = System.getenv("SMARTCITY_ADMIN_EMAIL");
        String password = System.getenv("SMARTCITY_ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.length() < 16) {
            System.out.println("[DB] Production mode: demo accounts disabled. Configure "
                    + "SMARTCITY_ADMIN_EMAIL and a 16+ character SMARTCITY_ADMIN_PASSWORD to seed an admin.");
            return;
        }

        String passwordHash = org.mindrot.jbcrypt.BCrypt.hashpw(
                password, org.mindrot.jbcrypt.BCrypt.gensalt(12));
        try (var ps = conn.prepareStatement(
                "INSERT INTO users (name, email, password, phone, role, civic_points, ward) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "System Administrator");
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setNull(4, java.sql.Types.VARCHAR);
            ps.setString(5, "admin");
            ps.setInt(6, 0);
            ps.setString(7, "Headquarters");
            ps.executeUpdate();
        }
        System.out.println("[DB] Production administrator account initialized.");
    }

    private void repairLegacyDemoPasswords(Connection conn) throws Exception {
        if ("production".equalsIgnoreCase(System.getenv("SMARTCITY_ENV"))) {
            return;
        }

        String legacyHash = "$2a$10$vI8aWBnW3fID.ZQ4/zo1e.uQx7Fzq52KjR0uL1y6K4L1d0n9H3G8G";
        String[][] demoAccounts = {
            {"admin@example.test", "admin123"},
            {"officer@example.test", "admin123"},
            {"citizen.one@example.test", "citizen123"},
            {"citizen.two@example.test", "citizen123"},
            {"citizen.three@example.test", "citizen123"}
        };
        boolean repaired = false;
        try (var ps = conn.prepareStatement(
                "UPDATE users SET password = ? WHERE email = ? AND password = ?")) {
            for (String[] account : demoAccounts) {
                ps.setString(1, org.mindrot.jbcrypt.BCrypt.hashpw(
                        account[1], org.mindrot.jbcrypt.BCrypt.gensalt(10)));
                ps.setString(2, account[0]);
                ps.setString(3, legacyHash);
                repaired |= ps.executeUpdate() > 0;
            }
        }
        if (repaired) {
            System.out.println("[DB] Repaired outdated local demo-account password hashes.");
        }
    }
}
