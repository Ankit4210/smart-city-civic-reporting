package com.smartcity.dao;

import com.smartcity.model.Report;
import com.smartcity.model.ActivityLog;
import com.smartcity.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Report Data Access Object (DAO)
 * Handles all JDBC operations for the 'reports', 'resolutions', 'upvotes',
 * 'feedback', and 'activity_logs' tables.
 */
public class ReportDAO {
    public record ReportSubmissionResult(int reportId, boolean duplicate, boolean upvoted) {}

    /**
     * Generate a unique Tracking ID (SSC-YYYY-XXXX).
     */
    public String generateTrackingId() {
        int year = java.time.Year.now().getValue();
        int random = 1000 + new Random().nextInt(9000);
        return "SSC-" + year + "-" + random;
    }

    /**
     * Submit a new civic issue report.
     * Returns the generated report ID.
     */
    public ReportSubmissionResult submitReport(int userId, String userName, int categoryId, String title,
                                               String description, String landmark, String address, String ward,
                                               double latitude, double longitude, String beforePhoto, String priority)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String reportWard = ward != null && !ward.isEmpty() ? ward : "Ward 12 - Central";
            double minLatitude = Math.max(-90, latitude - 0.005);
            double maxLatitude = Math.min(90, latitude + 0.005);
            double closestToEquator = minLatitude <= 0 && maxLatitude >= 0
                    ? 0 : Math.min(Math.abs(minLatitude), Math.abs(maxLatitude));
            double longitudeDelta = Math.min(180, 500.0 / (111320 * Math.max(
                    Math.abs(Math.cos(Math.toRadians(closestToEquator))), 0.000001)));
            ps = conn.prepareStatement(
                "SELECT r.id, r.user_id, r.tracking_id, r.latitude, r.longitude, " +
                "EXISTS(SELECT 1 FROM upvotes uv WHERE uv.report_id = r.id AND uv.user_id = ?) AS already_upvoted " +
                "FROM reports r WHERE r.category_id = ? AND r.ward = ? " +
                "AND r.latitude BETWEEN ? AND ? AND r.longitude BETWEEN ? AND ? " +
                "AND r.status IN ('Pending', 'In Progress') ORDER BY r.created_at ASC FOR UPDATE"
            );
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            ps.setString(3, reportWard);
            ps.setDouble(4, minLatitude);
            ps.setDouble(5, maxLatitude);
            ps.setDouble(6, Math.max(-180, longitude - longitudeDelta));
            ps.setDouble(7, Math.min(180, longitude + longitudeDelta));
            rs = ps.executeQuery();
            int duplicateReportId = 0;
            int duplicateOwnerId = 0;
            String duplicateTrackingId = null;
            boolean alreadyUpvoted = false;
            double nearestDistance = Double.MAX_VALUE;
            while (rs.next()) {
                double distance = distanceInMeters(latitude, longitude,
                        rs.getDouble("latitude"), rs.getDouble("longitude"));
                if (distance <= 500 && distance < nearestDistance) {
                    nearestDistance = distance;
                    duplicateReportId = rs.getInt("id");
                    duplicateOwnerId = rs.getInt("user_id");
                    duplicateTrackingId = rs.getString("tracking_id");
                    alreadyUpvoted = rs.getBoolean("already_upvoted");
                }

            }
            rs.close();
            ps.close();

            if (duplicateReportId > 0) {
                boolean upvoted = false;
                if (!alreadyUpvoted) {
                    ps = conn.prepareStatement("INSERT INTO upvotes (report_id, user_id) VALUES (?, ?)");
                    ps.setInt(1, duplicateReportId);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                    ps.close();

                    ps = conn.prepareStatement("UPDATE reports SET upvotes = upvotes + 1 WHERE id = ?");
                    ps.setInt(1, duplicateReportId);
                    ps.executeUpdate();
                    ps.close();

                    ps = conn.prepareStatement("UPDATE users SET civic_points = civic_points + 10 WHERE id = ?");
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                    ps.close();
                    upvoted = true;

                    if (duplicateOwnerId != userId) {
                        ps = conn.prepareStatement(
                                "INSERT INTO notifications (user_id, report_id, title, message, type) " +
                                "VALUES (?, ?, ?, ?, ?)");
                        ps.setInt(1, duplicateOwnerId);
                        ps.setInt(2, duplicateReportId);
                        ps.setString(3, "Your report received support");
                        ps.setString(4, "Another citizen confirmed a nearby issue. Tracking ID: " +
                                duplicateTrackingId + ".");
                        ps.setString(5, "issue_upvoted");
                        ps.executeUpdate();
                        ps.close();
                    }
                }

                ps = conn.prepareStatement(
                        "UPDATE reports SET priority = 'High' WHERE id = ? AND priority IN ('Low', 'Medium')");
                ps.setInt(1, duplicateReportId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement(
                        "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)");
                ps.setInt(1, duplicateReportId);
                ps.setString(2, userName);
                ps.setString(3, "Duplicate Report Matched");
                ps.setString(4, "A nearby " + title.trim() +
                        " report was matched to this active report instead of creating a duplicate.");
                ps.executeUpdate();
                ps.close();

                conn.commit();
                return new ReportSubmissionResult(duplicateReportId, true, upvoted);
            }

            String trackingId = generateTrackingId();
            // 1. Insert report
            ps = conn.prepareStatement(
                "INSERT INTO reports (tracking_id, user_id, category_id, title, description, " +
                "landmark, address, ward, latitude, longitude, before_photo, priority, status, upvotes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending', 1)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, trackingId);
            ps.setInt(2, userId);
            ps.setInt(3, categoryId);
            ps.setString(4, title.trim());
            ps.setString(5, description.trim());
            ps.setString(6, landmark != null ? landmark.trim() : "");
            ps.setString(7, address != null ? address.trim() : "Auto-detected Location");
            ps.setString(8, reportWard);
            ps.setDouble(9, latitude);
            ps.setDouble(10, longitude);
            ps.setString(11, beforePhoto != null ? beforePhoto : "default_before.jpg");
            String effectivePriority = priority == null ? "Medium" : priority;
            ps.setString(12, effectivePriority);
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            int reportId = 0;
            if (rs.next()) {
                reportId = rs.getInt(1);
            }
            rs.close();
            ps.close();

            // 2. Self upvote
            ps = conn.prepareStatement("INSERT INTO upvotes (report_id, user_id) VALUES (?, ?)");
            ps.setInt(1, reportId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            ps.close();

            // 3. Activity log
            ps = conn.prepareStatement(
                "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, reportId);
            ps.setString(2, userName);
            ps.setString(3, "Report Submitted");
            ps.setString(4, "Civic complaint registered with Tracking ID: " + trackingId);
            ps.executeUpdate();
            ps.close();

            // 4. Award +50 Civic Points
            ps = conn.prepareStatement("UPDATE users SET civic_points = civic_points + 50 WHERE id = ?");
            ps.setInt(1, userId);
            ps.executeUpdate();
            ps.close();

            // 5. Notification
            ps = conn.prepareStatement(
                "INSERT INTO notifications (user_id, report_id, title, message, type) VALUES (?, ?, ?, ?, ?)"
            );
            ps.setInt(1, userId);
            ps.setInt(2, reportId);
            ps.setString(3, "Report Registered Successfully");
            ps.setString(4, "Your complaint '" + title.trim() + "' has been registered. Tracking ID: " + trackingId + ". +50 Civic Points awarded!");
            ps.setString(5, "issue_created");
            ps.executeUpdate();

            conn.commit();
            return new ReportSubmissionResult(reportId, false, false);

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                DBUtil.closeConnection(conn);
            }
        }
    }

    private static double distanceInMeters(double firstLatitude, double firstLongitude,
                                           double secondLatitude, double secondLongitude) {
        double latitudeDelta = Math.toRadians(secondLatitude - firstLatitude);
        double longitudeDelta = Math.toRadians(secondLongitude - firstLongitude);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(Math.toRadians(firstLatitude)) * Math.cos(Math.toRadians(secondLatitude))
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Get all issues with filters (for Explore page).
     */
    public List<Report> getAllIssues(String categoryId, String status, String ward, String search, String sort)
            throws SQLException {
        return getAllIssues(categoryId, status, ward, search, sort, 0);
    }

    public List<Report> getAllIssues(String categoryId, String status, String ward, String search, String sort,
                                     int currentUserId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Report> issues = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();

            StringBuilder sql = new StringBuilder(
                "SELECT r.*, c.name as category_name, c.icon as category_icon, c.sla_hours, " +
                "c.department as department_name, u.name as reporter_name " +
                (currentUserId > 0
                        ? ", EXISTS(SELECT 1 FROM upvotes uv WHERE uv.report_id = r.id AND uv.user_id = ?) AS has_upvoted "
                        : ", 0 AS has_upvoted ") +
                "FROM reports r " +
                "JOIN categories c ON r.category_id = c.id " +
                "JOIN users u ON r.user_id = u.id WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();
            if (currentUserId > 0) {
                params.add(currentUserId);
            }

            if (categoryId != null && !categoryId.isEmpty() && !categoryId.equals("all")) {
                sql.append(" AND r.category_id = ?");
                params.add(Integer.parseInt(categoryId));
            }
            if (status != null && !status.isEmpty() && !status.equals("all")) {
                sql.append(" AND r.status = ?");
                params.add(status);
            }
            if (ward != null && !ward.isEmpty() && !ward.equals("all")) {
                sql.append(" AND r.ward = ?");
                params.add(ward);
            }
            if (search != null && !search.trim().isEmpty()) {
                sql.append(" AND (r.title LIKE ? OR r.description LIKE ? OR r.address LIKE ? OR r.tracking_id LIKE ?)");
                String term = "%" + search.trim() + "%";
                params.add(term);
                params.add(term);
                params.add(term);
                params.add(term);
            }

            if ("upvotes".equals(sort)) {
                sql.append(" ORDER BY r.upvotes DESC, r.created_at DESC");
            } else if ("oldest".equals(sort)) {
                sql.append(" ORDER BY r.created_at ASC");
            } else {
                sql.append(" ORDER BY r.created_at DESC");
            }

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                Report report = extractReportBasic(rs);
                report.setHasUpvoted(rs.getBoolean("has_upvoted"));
                issues.add(report);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return issues;
    }

    /**
     * Get reports by a specific user (My Reports).
     */
    public List<Report> getReportsByUser(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Report> reports = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT r.*, c.name as category_name, c.icon as category_icon, c.sla_hours, " +
                "c.department as department_name, " +
                "res.after_photo, res.remarks as resolution_remarks, res.resolved_at, " +
                "f.rating as feedback_rating, f.is_satisfied as feedback_satisfied, f.comments as feedback_comments, " +
                "EXISTS(SELECT 1 FROM upvotes uv WHERE uv.report_id = r.id AND uv.user_id = r.user_id) AS has_upvoted " +
                "FROM reports r " +
                "JOIN categories c ON r.category_id = c.id " +
                "LEFT JOIN resolutions res ON r.id = res.report_id " +
                "LEFT JOIN feedback f ON r.id = f.report_id " +
                "WHERE r.user_id = ? ORDER BY r.created_at DESC"
            );
            ps.setInt(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                Report report = extractReportBasic(rs);
                report.setAfterPhoto(rs.getString("after_photo"));
                report.setResolutionRemarks(rs.getString("resolution_remarks"));
                report.setResolvedAt(rs.getTimestamp("resolved_at"));
                report.setFeedbackRating(rs.getInt("feedback_rating"));
                report.setFeedbackSatisfied(rs.getInt("feedback_satisfied") == 1);
                report.setFeedbackComments(rs.getString("feedback_comments"));
                report.setHasUpvoted(rs.getBoolean("has_upvoted"));
                report.setTimeline(getActivityTimeline(conn, report.getId()));
                reports.add(report);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return reports;
    }

    /**
     * Track a report by Tracking ID (public feature).
     */
    public Report trackByTrackingId(String trackingId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT r.*, c.name as category_name, c.icon as category_icon, c.sla_hours, " +
                "c.department as department_name, u.name as reporter_name, " +
                "res.after_photo, res.remarks as resolution_remarks, res.action_taken, " +
                "res.resolved_at, admin.name as resolved_by_admin, " +
                "f.rating as feedback_rating, f.is_satisfied as feedback_satisfied, f.comments as feedback_comments " +
                "FROM reports r " +
                "JOIN categories c ON r.category_id = c.id " +
                "JOIN users u ON r.user_id = u.id " +
                "LEFT JOIN resolutions res ON r.id = res.report_id " +
                "LEFT JOIN users admin ON res.admin_id = admin.id " +
                "LEFT JOIN feedback f ON r.id = f.report_id " +
                "WHERE UPPER(r.tracking_id) = UPPER(?)"
            );
            ps.setString(1, trackingId.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                Report report = extractReportBasic(rs);
                report.setReporterName(rs.getString("reporter_name"));
                report.setAfterPhoto(rs.getString("after_photo"));
                report.setResolutionRemarks(rs.getString("resolution_remarks"));
                report.setActionTaken(rs.getString("action_taken"));
                report.setResolvedAt(rs.getTimestamp("resolved_at"));
                report.setResolvedByAdmin(rs.getString("resolved_by_admin"));
                report.setFeedbackRating(rs.getInt("feedback_rating"));
                report.setFeedbackSatisfied(rs.getInt("feedback_satisfied") == 1);
                report.setFeedbackComments(rs.getString("feedback_comments"));
                report.setTimeline(getActivityTimeline(conn, report.getId()));
                return report;
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return null;
    }

    /**
     * Get report by ID.
     */
    public Report getReportById(int id) throws SQLException {
        return getReportById(id, 0);
    }

    public Report getReportById(int id, int currentUserId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT r.*, c.name as category_name, c.icon as category_icon, c.sla_hours, " +
                "c.department as department_name, u.name as reporter_name, " +
                "res.after_photo, res.remarks as resolution_remarks, res.action_taken, res.resolved_at, " +
                "f.rating as feedback_rating, f.is_satisfied as feedback_satisfied, f.comments as feedback_comments, " +
                (currentUserId > 0
                        ? "EXISTS(SELECT 1 FROM upvotes uv WHERE uv.report_id = r.id AND uv.user_id = ?) AS has_upvoted "
                        : "0 AS has_upvoted ") +
                "FROM reports r JOIN categories c ON r.category_id = c.id " +
                "JOIN users u ON r.user_id = u.id " +
                "LEFT JOIN resolutions res ON r.id = res.report_id " +
                "LEFT JOIN feedback f ON r.id = f.report_id WHERE r.id = ?");
            if (currentUserId > 0) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, id);
            } else {
                ps.setInt(1, id);
            }
            rs = ps.executeQuery();

            if (rs.next()) {
                Report report = extractReportBasic(rs);
                report.setReporterName(rs.getString("reporter_name"));
                report.setAfterPhoto(rs.getString("after_photo"));
                report.setResolutionRemarks(rs.getString("resolution_remarks"));
                report.setActionTaken(rs.getString("action_taken"));
                report.setResolvedAt(rs.getTimestamp("resolved_at"));
                report.setFeedbackRating(rs.getInt("feedback_rating"));
                report.setFeedbackSatisfied(rs.getInt("feedback_satisfied") == 1);
                report.setFeedbackComments(rs.getString("feedback_comments"));
                report.setHasUpvoted(rs.getBoolean("has_upvoted"));
                report.setTimeline(getActivityTimeline(conn, report.getId()));
                return report;
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return null;
    }

    /**
     * Toggle upvote on a report.
     * Returns true if upvoted, false if removed.
     */
    public boolean toggleUpvote(int reportId, int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // Check existing upvote
            ps = conn.prepareStatement("SELECT id FROM upvotes WHERE report_id = ? AND user_id = ?");
            ps.setInt(1, reportId);
            ps.setInt(2, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                int upvoteId = rs.getInt("id");
                rs.close();
                ps.close();

                // Remove upvote
                ps = conn.prepareStatement("DELETE FROM upvotes WHERE id = ?");
                ps.setInt(1, upvoteId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("UPDATE reports SET upvotes = GREATEST(1, upvotes - 1) WHERE id = ?");
                ps.setInt(1, reportId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("UPDATE users SET civic_points = GREATEST(0, civic_points - 10) WHERE id = ?");
                ps.setInt(1, userId);
                ps.executeUpdate();

                conn.commit();
                return false; // Upvote removed
            } else {
                rs.close();
                ps.close();

                // Add upvote
                ps = conn.prepareStatement("INSERT INTO upvotes (report_id, user_id) VALUES (?, ?)");
                ps.setInt(1, reportId);
                ps.setInt(2, userId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("UPDATE reports SET upvotes = upvotes + 1 WHERE id = ?");
                ps.setInt(1, reportId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement("UPDATE users SET civic_points = civic_points + 10 WHERE id = ?");
                ps.setInt(1, userId);
                ps.executeUpdate();
                ps.close();

                // Auto elevate priority if upvotes >= 10
                ps = conn.prepareStatement("UPDATE reports SET priority = 'High' WHERE id = ? AND upvotes >= 10 AND priority = 'Medium'");
                ps.setInt(1, reportId);
                ps.executeUpdate();

                conn.commit();
                return true; // Upvoted
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                DBUtil.closeConnection(conn);
            }
        }
    }

    /**
     * Submit feedback & rating (Post-resolution).
     * Returns true if issue was reopened (not satisfied), false otherwise.
     */
    public boolean submitFeedback(int reportId, int userId, String userName, int rating, boolean isSatisfied, String comments)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // Check existing feedback
            ps = conn.prepareStatement("SELECT id FROM feedback WHERE report_id = ?");
            ps.setInt(1, reportId);
            rs = ps.executeQuery();

            boolean isNewFeedback = !rs.next();
            if (!isNewFeedback) {
                int feedbackId = rs.getInt("id");
                rs.close();
                ps.close();
                ps = conn.prepareStatement(
                    "UPDATE feedback SET rating = ?, is_satisfied = ?, comments = ? WHERE id = ?"
                );
                ps.setInt(1, rating);
                ps.setInt(2, isSatisfied ? 1 : 0);
                ps.setString(3, comments != null ? comments : "");
                ps.setInt(4, feedbackId);
                ps.executeUpdate();
            } else {
                rs.close();
                ps.close();
                ps = conn.prepareStatement(
                    "INSERT INTO feedback (report_id, user_id, rating, is_satisfied, comments) VALUES (?, ?, ?, ?, ?)"
                );
                ps.setInt(1, reportId);
                ps.setInt(2, userId);
                ps.setInt(3, rating);
                ps.setInt(4, isSatisfied ? 1 : 0);
                ps.setString(5, comments != null ? comments : "");
                ps.executeUpdate();
            }
            ps.close();

            if (!isSatisfied) {
                // Reopen the issue
                ps = conn.prepareStatement(
                    "UPDATE reports SET status = 'In Progress', updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                );
                ps.setInt(1, reportId);
                ps.executeUpdate();
                ps.close();

                ps = conn.prepareStatement(
                    "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)"
                );
                ps.setInt(1, reportId);
                ps.setString(2, userName);
                ps.setString(3, "Issue Reopened");
                ps.setString(4, "Citizen reported unsatisfactory resolution: \"" +
                    (comments != null ? comments : "Requires re-inspection") + "\"");
                ps.executeUpdate();

                conn.commit();
                return true; // Reopened
            }

            if (isNewFeedback) {
                ps = conn.prepareStatement("UPDATE users SET civic_points = civic_points + 20 WHERE id = ?");
                ps.setInt(1, userId);
                ps.executeUpdate();
                ps.close();
            }

            ps = conn.prepareStatement(
                "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, reportId);
            ps.setString(2, userName);
            ps.setString(3, "Feedback Submitted");
            ps.setString(4, "Citizen rated resolution: " + rating + "/5 Stars (Satisfied)");
            ps.executeUpdate();

            conn.commit();
            return false; // Not reopened

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                DBUtil.closeConnection(conn);
            }
        }
    }

    // ===================== ADMIN OPERATIONS =====================

    /**
     * Get admin dashboard stats.
     */
    public int[] getDashboardStats() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            stmt = conn.createStatement();

            int[] stats = new int[8]; // total, pending, inProgress, resolved, critical, citizens, rate, high priority

            rs = stmt.executeQuery("SELECT COUNT(*) FROM reports");
            if (rs.next()) stats[0] = rs.getInt(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM reports WHERE status = 'Pending'");
            if (rs.next()) stats[1] = rs.getInt(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM reports WHERE status = 'In Progress'");
            if (rs.next()) stats[2] = rs.getInt(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM reports WHERE status = 'Resolved'");
            if (rs.next()) stats[3] = rs.getInt(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM reports WHERE priority = 'Critical' AND status != 'Resolved'");
            if (rs.next()) stats[4] = rs.getInt(1);
            rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'citizen'");
            if (rs.next()) stats[5] = rs.getInt(1);
            rs.close();

            stats[6] = stats[0] > 0 ? Math.round((float) stats[3] / stats[0] * 100) : 0;

            rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM reports WHERE priority IN ('High', 'Critical') AND status != 'Resolved'"
            );
            if (rs.next()) stats[7] = rs.getInt(1);

            return stats;
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            DBUtil.closeConnection(conn);
        }
    }

    /**
     * Get all reports for admin (with optional filters).
     */
    public List<Report> getAdminReports(String statusFilter, String categoryFilter) throws SQLException {
        return getAdminReports(statusFilter, categoryFilter, null);
    }

    public List<Report> getAdminReports(String statusFilter, String categoryFilter, String wardFilter)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Report> reports = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            StringBuilder sql = new StringBuilder(
                "SELECT r.*, c.name as category_name, c.icon as category_icon, c.sla_hours, " +
                "c.department as department_name, u.name as reporter_name " +
                "FROM reports r " +
                "JOIN categories c ON r.category_id = c.id " +
                "JOIN users u ON r.user_id = u.id WHERE 1=1 "
            );

            List<Object> params = new ArrayList<>();
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals("all")) {
                sql.append(" AND r.status = ?");
                params.add(statusFilter);
            }
            if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryFilter.equals("all")) {
                sql.append(" AND r.category_id = ?");
                params.add(Integer.parseInt(categoryFilter));
            }
            if (wardFilter != null && !wardFilter.isEmpty() && !wardFilter.equals("all")) {
                sql.append(" AND r.ward = ?");
                params.add(wardFilter);
            }
            sql.append(" ORDER BY FIELD(r.priority, 'Critical', 'High', 'Medium', 'Low'), r.created_at DESC");

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                Report r = extractReportBasic(rs);
                r.setReporterName(rs.getString("reporter_name"));
                reports.add(r);
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return reports;
    }

    /**
     * Update report status (Admin).
     */
    public void updateStatus(int reportId, String status, String remarks, String priority, String adminName)
            throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // Get report info for notification
            Report report = getReportById(reportId);
            if (report == null) throw new SQLException("Report not found");

            StringBuilder sql = new StringBuilder("UPDATE reports SET status = ?, updated_at = CURRENT_TIMESTAMP");
            List<Object> params = new ArrayList<>();
            params.add(status);

            if (priority != null && !priority.isEmpty()) {
                sql.append(", priority = ?");
                params.add(priority);
            }
            sql.append(" WHERE id = ?");
            params.add(reportId);

            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                } else {
                    ps.setString(i + 1, (String) param);
                }
            }
            ps.executeUpdate();
            ps.close();

            // Activity log
            ps = conn.prepareStatement(
                "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, reportId);
            ps.setString(2, adminName);
            ps.setString(3, "Status Changed to " + status);
            ps.setString(4, remarks != null ? remarks : "Status transitioned to " + status);
            ps.executeUpdate();
            ps.close();

            // Notify reporter
            ps = conn.prepareStatement(
                "INSERT INTO notifications (user_id, report_id, title, message, type) VALUES (?, ?, ?, ?, ?)"
            );
            ps.setInt(1, report.getUserId());
            ps.setInt(2, reportId);
            ps.setString(3, "Status Update: " + status);
            ps.setString(4, "Your complaint '" + report.getTitle() + "' status is now '" + status + "'. Remarks: " +
                (remarks != null ? remarks : "Processing underway."));
            ps.setString(5, "status_update");
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (ps != null) ps.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                DBUtil.closeConnection(conn);
            }
        }
    }

    /**
     * Resolve an issue with after-photo proof (Admin).
     */
    public void resolveIssue(int reportId, int adminId, String adminName, String afterPhoto,
                             String remarks, String actionTaken) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Report report = getReportById(reportId);
            if (report == null) throw new SQLException("Report not found");

            // Check existing resolution
            ps = conn.prepareStatement("SELECT id FROM resolutions WHERE report_id = ?");
            ps.setInt(1, reportId);
            rs = ps.executeQuery();

            if (rs.next()) {
                int resId = rs.getInt("id");
                rs.close();
                ps.close();
                ps = conn.prepareStatement(
                    "UPDATE resolutions SET admin_id = ?, after_photo = ?, remarks = ?, action_taken = ?, resolved_at = CURRENT_TIMESTAMP WHERE id = ?"
                );
                ps.setInt(1, adminId);
                ps.setString(2, afterPhoto);
                ps.setString(3, remarks != null ? remarks : "Issue successfully rectified.");
                ps.setString(4, actionTaken != null ? actionTaken : "Site Maintenance");
                ps.setInt(5, resId);
                ps.executeUpdate();
            } else {
                rs.close();
                ps.close();
                ps = conn.prepareStatement(
                    "INSERT INTO resolutions (report_id, admin_id, after_photo, remarks, action_taken) VALUES (?, ?, ?, ?, ?)"
                );
                ps.setInt(1, reportId);
                ps.setInt(2, adminId);
                ps.setString(3, afterPhoto);
                ps.setString(4, remarks != null ? remarks : "Issue successfully rectified.");
                ps.setString(5, actionTaken != null ? actionTaken : "Site Maintenance");
                ps.executeUpdate();
            }
            ps.close();

            // Mark resolved
            ps = conn.prepareStatement("UPDATE reports SET status = 'Resolved', updated_at = CURRENT_TIMESTAMP WHERE id = ?");
            ps.setInt(1, reportId);
            ps.executeUpdate();
            ps.close();

            // Activity log
            ps = conn.prepareStatement(
                "INSERT INTO activity_logs (report_id, actor_name, action, details) VALUES (?, ?, ?, ?)"
            );
            ps.setInt(1, reportId);
            ps.setString(2, adminName);
            ps.setString(3, "Issue Resolved");
            ps.setString(4, "After-Photo uploaded. Remarks: \"" + (remarks != null ? remarks : "Issue resolved.") + "\"");
            ps.executeUpdate();
            ps.close();

            // Award +25 Civic Points to citizen
            ps = conn.prepareStatement("UPDATE users SET civic_points = civic_points + 25 WHERE id = ?");
            ps.setInt(1, report.getUserId());
            ps.executeUpdate();
            ps.close();

            // Notify citizen
            ps = conn.prepareStatement(
                "INSERT INTO notifications (user_id, report_id, title, message, type) VALUES (?, ?, ?, ?, ?)"
            );
            ps.setInt(1, report.getUserId());
            ps.setInt(2, reportId);
            ps.setString(3, "Issue Resolved with Proof!");
            ps.setString(4, "Your reported issue '" + report.getTitle() + "' has been Resolved. View after-photo proof and share your feedback! +25 bonus points awarded.");
            ps.setString(5, "resolved");
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                DBUtil.closeConnection(conn);
            }
        }
    }

    /**
     * Get category-wise analytics.
     */
    public List<Object[]> getCategoryAnalytics() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Object[]> data = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement(
                "SELECT c.name, COUNT(r.id) as count FROM categories c " +
                "LEFT JOIN reports r ON c.id = r.category_id GROUP BY c.id"
            );
            rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getString("name"), rs.getInt("count")});
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return data;
    }

    /**
     * Get status-wise analytics.
     */
    public List<Object[]> getStatusAnalytics() throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Object[]> data = new ArrayList<>();

        try {
            conn = DBUtil.getConnection();
            ps = conn.prepareStatement("SELECT status, COUNT(*) as count FROM reports GROUP BY status");
            rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getString("status"), rs.getInt("count")});
            }
        } finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            DBUtil.closeConnection(conn);
        }
        return data;
    }

    public List<Map<String, Object>> getAreaAnalytics() throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = "SELECT ward, COUNT(*) AS report_count, " +
                "SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending_count, " +
                "SUM(CASE WHEN status = 'Resolved' THEN 1 ELSE 0 END) AS resolved_count " +
                "FROM reports GROUP BY ward ORDER BY report_count DESC, ward";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> area = new LinkedHashMap<>();
                area.put("ward", rs.getString("ward"));
                area.put("report_count", rs.getInt("report_count"));
                area.put("pending_count", rs.getInt("pending_count"));
                area.put("resolved_count", rs.getInt("resolved_count"));
                data.add(area);
            }
        }
        return data;
    }

    public List<Map<String, Object>> getAverageResolutionTimeByCategory() throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = "SELECT c.name AS category, AVG(TIMESTAMPDIFF(SECOND, r.created_at, res.resolved_at) / 3600.0) " +
                "AS average_hours, COUNT(res.id) AS resolved_reports " +
                "FROM categories c LEFT JOIN reports r ON r.category_id = c.id " +
                "LEFT JOIN resolutions res ON res.report_id = r.id " +
                "GROUP BY c.id, c.name ORDER BY c.name";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> category = new LinkedHashMap<>();
                category.put("category", rs.getString("category"));
                category.put("average_hours", rs.getObject("average_hours"));
                category.put("resolved_reports", rs.getInt("resolved_reports"));
                data.add(category);
            }
        }
        return data;
    }

    public List<Map<String, Object>> getHotspots() throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = "SELECT r.ward, c.name AS category_name, c.icon AS category_icon, " +
                "ROUND(AVG(r.latitude), 6) AS center_lat, ROUND(AVG(r.longitude), 6) AS center_lng, " +
                "COUNT(*) AS issue_count, " +
                "SUM(CASE WHEN r.priority = 'Critical' THEN 1 ELSE 0 END) AS critical_count " +
                "FROM reports r JOIN categories c ON c.id = r.category_id " +
                "WHERE r.status IN ('Pending', 'In Progress') " +
                "GROUP BY r.category_id, c.name, c.icon, r.ward, " +
                "ROUND(r.latitude, 3), ROUND(r.longitude, 3) " +
                "HAVING COUNT(*) > 1 ORDER BY issue_count DESC, critical_count DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> hotspot = new LinkedHashMap<>();
                hotspot.put("ward", rs.getString("ward"));
                hotspot.put("category_name", rs.getString("category_name"));
                hotspot.put("category_icon", rs.getString("category_icon"));
                hotspot.put("center_lat", rs.getDouble("center_lat"));
                hotspot.put("center_lng", rs.getDouble("center_lng"));
                hotspot.put("issue_count", rs.getInt("issue_count"));
                hotspot.put("critical_count", rs.getInt("critical_count"));
                data.add(hotspot);
            }
        }
        return data;
    }

    /**
     * Helper: Extract basic report from ResultSet.
     */
    private Report extractReportBasic(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setTrackingId(rs.getString("tracking_id"));
        report.setUserId(rs.getInt("user_id"));
        report.setCategoryId(rs.getInt("category_id"));
        report.setSlaHours(rs.getInt("sla_hours"));
        report.setTitle(rs.getString("title"));
        report.setDescription(rs.getString("description"));
        report.setLandmark(rs.getString("landmark"));
        report.setAddress(rs.getString("address"));
        report.setWard(rs.getString("ward"));
        report.setLatitude(rs.getDouble("latitude"));
        report.setLongitude(rs.getDouble("longitude"));
        report.setBeforePhoto(rs.getString("before_photo"));
        report.setPriority(rs.getString("priority"));
        report.setStatus(rs.getString("status"));
        report.setUpvotes(rs.getInt("upvotes"));
        report.setCreatedAt(rs.getTimestamp("created_at"));
        report.setUpdatedAt(rs.getTimestamp("updated_at"));

        // Try to get joined fields (may not be present in all queries)
        try { report.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
        try { report.setCategoryIcon(rs.getString("category_icon")); } catch (SQLException ignored) {}
        try { report.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}

        return report;
    }

    private List<ActivityLog> getActivityTimeline(Connection conn, int reportId) throws SQLException {
        List<ActivityLog> timeline = new ArrayList<>();
        String sql = "SELECT actor_name, action, details, created_at FROM activity_logs " +
                "WHERE report_id = ? ORDER BY created_at ASC, id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLog entry = new ActivityLog();
                    entry.setActorName(rs.getString("actor_name"));
                    entry.setAction(rs.getString("action"));
                    entry.setDetails(rs.getString("details"));
                    entry.setCreatedAt(rs.getTimestamp("created_at"));
                    timeline.add(entry);
                }
            }
        }
        return timeline;
    }
}
