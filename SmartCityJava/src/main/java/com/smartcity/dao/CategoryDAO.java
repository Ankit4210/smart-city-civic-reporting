package com.smartcity.dao;

import com.smartcity.model.Category;
import com.smartcity.model.Notification;
import com.smartcity.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Category & Notification DAO
 * Handles JDBC operations for categories and notifications tables.
 */
public class CategoryDAO {

    /**
     * Get all civic issue categories.
     */
    public List<Category> getAllCategories() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Category> categories = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT * FROM categories ORDER BY id ASC");
            rs = ps.executeQuery();

            while (rs.next()) {
                Category cat = new Category();
                cat.setId(rs.getInt("id"));
                cat.setName(rs.getString("name"));
                cat.setCode(rs.getString("code"));
                cat.setIcon(rs.getString("icon"));
                cat.setDepartment(rs.getString("department"));
                cat.setSlaHours(rs.getInt("sla_hours"));
                cat.setDescription(rs.getString("description"));
                categories.add(cat);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return categories;
    }

    /**
     * Get notifications for a user.
     */
    public List<Notification> getNotifications(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Notification> notifications = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT 30"
            );
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Notification n = new Notification();
                n.setId(rs.getInt("id"));
                n.setUserId(rs.getInt("user_id"));
                n.setReportId(rs.getInt("report_id"));
                n.setTitle(rs.getString("title"));
                n.setMessage(rs.getString("message"));
                n.setType(rs.getString("type"));
                n.setRead(rs.getInt("is_read") == 1);
                n.setCreatedAt(rs.getTimestamp("created_at"));
                notifications.add(n);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return notifications;
    }

    /**
     * Get unread notification count.
     */
    public int getUnreadCount(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0"
            );
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return 0;
    }

    /**
     * Mark a single notification as read.
     */
    public void markAsRead(int notificationId, int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("UPDATE notifications SET is_read = 1 WHERE id = ? AND user_id = ?");
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
    }

    /**
     * Mark all notifications as read for a user.
     */
    public void markAllAsRead(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("UPDATE notifications SET is_read = 1 WHERE user_id = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();
        } finally {
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
    }
}
