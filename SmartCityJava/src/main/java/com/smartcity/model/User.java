package com.smartcity.model;

import java.sql.Timestamp;

/**
 * User Model (Citizen & Municipal Admin)
 * Maps to the 'users' table in smart_city_db.
 */
public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String role;       // citizen, admin
    private int civicPoints;
    private String ward;
    private Timestamp createdAt;

    // Computed fields (not stored in DB)
    private int totalReported;
    private int totalResolved;

    public User() {}

    public User(int id, String name, String email, String role, int civicPoints, String ward) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.civicPoints = civicPoints;
        this.ward = ward;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getCivicPoints() { return civicPoints; }
    public void setCivicPoints(int civicPoints) { this.civicPoints = civicPoints; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getTotalReported() { return totalReported; }
    public void setTotalReported(int totalReported) { this.totalReported = totalReported; }

    public int getTotalResolved() { return totalResolved; }
    public void setTotalResolved(int totalResolved) { this.totalResolved = totalResolved; }

    public boolean isAdmin() {
        return "admin".equals(this.role);
    }
}
