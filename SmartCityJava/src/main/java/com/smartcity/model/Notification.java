package com.smartcity.model;

import java.sql.Timestamp;

/**
 * Notification Model (Citizen Status & Gamification Alerts)
 * Maps to the 'notifications' table in smart_city_db.
 */
public class Notification {
    private int id;
    private int userId;
    private int reportId;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private Timestamp createdAt;

    public Notification() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
