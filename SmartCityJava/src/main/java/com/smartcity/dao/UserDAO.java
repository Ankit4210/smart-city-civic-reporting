package com.smartcity.dao;

import com.smartcity.model.User;
import com.smartcity.util.DBUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User Data Access Object (DAO)
 * Handles all JDBC operations for the 'users' table.
 */
public class UserDAO {

    /**
     * Register a new citizen user.
     * Returns the created User with generated ID, or null on failure.
     */
    public User register(String name, String email, String password, String phone, String ward) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();

            // Check if email already exists
            ps = conn.prepareStatement("SELECT id FROM users WHERE email = ?");
            ps.setString(1, email.toLowerCase().trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return null; // Email already taken
            }
            rs.close();
            ps.close();

            // Hash password using BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(10));

            // Insert new user
            ps = conn.prepareStatement(
                "INSERT INTO users (name, email, password, phone, role, civic_points, ward) VALUES (?, ?, ?, ?, 'citizen', 50, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name.trim());
            ps.setString(2, email.toLowerCase().trim());
            ps.setString(3, hashedPassword);
            ps.setString(4, phone != null ? phone : "");
            ps.setString(5, ward != null && !ward.isEmpty() ? ward : "Ward 12 - Central");
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt(1));
                user.setName(name.trim());
                user.setEmail(email.toLowerCase().trim());
                user.setRole("citizen");
                user.setCivicPoints(50);
                user.setWard(ward != null && !ward.isEmpty() ? ward : "Ward 12 - Central");
                return user;
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return null;
    }

    /**
     * Authenticate a user by email and password.
     * Returns User object if credentials match, null otherwise.
     */
    public User login(String email, String password) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            ps.setString(1, email.toLowerCase().trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password");
                if (BCrypt.checkpw(password, storedHash)) {
                    return extractUser(rs);
                }
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return null;
    }

    /**
     * Get user by ID.
     */
    public User getUserById(int id) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT u.*, " +
                "(SELECT COUNT(*) FROM reports WHERE user_id = u.id) as total_reported, " +
                "(SELECT COUNT(*) FROM reports WHERE user_id = u.id AND status = 'Resolved') as total_resolved " +
                "FROM users u WHERE u.id = ?"
            );
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs.next()) {
                User user = extractUser(rs);
                user.setTotalReported(rs.getInt("total_reported"));
                user.setTotalResolved(rs.getInt("total_resolved"));
                return user;
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return null;
    }

    /**
     * Update civic points for a user.
     */
    public void updateCivicPoints(int userId, int pointsToAdd) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "UPDATE users SET civic_points = GREATEST(0, civic_points + ?) WHERE id = ?"
            );
            ps.setInt(1, pointsToAdd);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
    }

    /**
     * Get top citizens by civic points for leaderboard.
     */
    public List<User> getLeaderboard(int limit) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<User> leaders = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT u.id, u.name, u.ward, u.civic_points, " +
                "COUNT(r.id) as total_reported, " +
                "SUM(CASE WHEN r.status = 'Resolved' THEN 1 ELSE 0 END) as total_resolved " +
                "FROM users u LEFT JOIN reports r ON u.id = r.user_id " +
                "WHERE u.role = 'citizen' " +
                "GROUP BY u.id ORDER BY u.civic_points DESC LIMIT ?"
            );
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setName(rs.getString("name"));
                user.setWard(rs.getString("ward"));
                user.setCivicPoints(rs.getInt("civic_points"));
                user.setTotalReported(rs.getInt("total_reported"));
                user.setTotalResolved(rs.getInt("total_resolved"));
                leaders.add(user);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return leaders;
    }

    /**
     * Helper: Extract User from ResultSet row.
     */
    private User extractUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setPhone(rs.getString("phone"));
        user.setRole(rs.getString("role"));
        user.setCivicPoints(rs.getInt("civic_points"));
        user.setWard(rs.getString("ward"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }
}
