package com.smartcity.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Report Model (Civic Issue Complaint)
 * Maps to the 'reports' table in smart_city_db.
 */
public class Report {
    private int id;
    private String trackingId;
    private int userId;
    private int categoryId;
    private int slaHours;
    private String title;
    private String description;
    private String landmark;
    private String address;
    private String ward;
    private double latitude;
    private double longitude;
    private String beforePhoto;
    private String priority;     // Low, Medium, High, Critical
    private String status;       // Pending, In Progress, Resolved, Rejected
    private int upvotes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Joined fields (from categories & users tables)
    private String categoryName;
    private String categoryIcon;
    private String departmentName;
    private String reporterName;

    // Resolution fields (from resolutions table)
    private String afterPhoto;
    private String resolutionRemarks;
    private String actionTaken;
    private Timestamp resolvedAt;
    private String resolvedByAdmin;

    // Feedback fields (from feedback table)
    private int feedbackRating;
    private boolean feedbackSatisfied;
    private String feedbackComments;
    private List<ActivityLog> timeline = new ArrayList<>();

    // User interaction
    private boolean hasUpvoted;

    public Report() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getSlaHours() { return slaHours; }
    public void setSlaHours(int slaHours) { this.slaHours = slaHours; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getBeforePhoto() { return beforePhoto; }
    public void setBeforePhoto(String beforePhoto) { this.beforePhoto = beforePhoto; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getAfterPhoto() { return afterPhoto; }
    public void setAfterPhoto(String afterPhoto) { this.afterPhoto = afterPhoto; }

    public String getResolutionRemarks() { return resolutionRemarks; }
    public void setResolutionRemarks(String resolutionRemarks) { this.resolutionRemarks = resolutionRemarks; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolvedByAdmin() { return resolvedByAdmin; }
    public void setResolvedByAdmin(String resolvedByAdmin) { this.resolvedByAdmin = resolvedByAdmin; }

    public int getFeedbackRating() { return feedbackRating; }
    public void setFeedbackRating(int feedbackRating) { this.feedbackRating = feedbackRating; }

    public boolean isFeedbackSatisfied() { return feedbackSatisfied; }
    public void setFeedbackSatisfied(boolean feedbackSatisfied) { this.feedbackSatisfied = feedbackSatisfied; }

    public String getFeedbackComments() { return feedbackComments; }
    public void setFeedbackComments(String feedbackComments) { this.feedbackComments = feedbackComments; }

    public List<ActivityLog> getTimeline() { return timeline; }
    public void setTimeline(List<ActivityLog> timeline) { this.timeline = timeline; }

    public boolean isHasUpvoted() { return hasUpvoted; }
    public void setHasUpvoted(boolean hasUpvoted) { this.hasUpvoted = hasUpvoted; }

    /**
     * Returns the CSS class for the status badge.
     */
    public String getStatusClass() {
        if (status == null) return "pending";
        return switch (status) {
            case "Pending" -> "pending";
            case "In Progress" -> "in-progress";
            case "Resolved" -> "resolved";
            case "Rejected" -> "rejected";
            default -> "pending";
        };
    }

    /**
     * Returns the CSS class for the priority badge.
     */
    public String getPriorityClass() {
        if (priority == null) return "medium";
        return switch (priority) {
            case "Low" -> "low";
            case "Medium" -> "medium";
            case "High" -> "high";
            case "Critical" -> "critical";
            default -> "medium";
        };
    }
}
