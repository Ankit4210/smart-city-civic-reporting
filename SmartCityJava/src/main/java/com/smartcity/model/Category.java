package com.smartcity.model;

/**
 * Category Model (Civic Issue Classification)
 * Maps to the 'categories' table in smart_city_db.
 */
public class Category {
    private int id;
    private String name;
    private String code;
    private String icon;
    private String department;
    private int slaHours;
    private String description;

    public Category() {}

    public Category(int id, String name, String code, String icon, String department, int slaHours, String description) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.icon = icon;
        this.department = department;
        this.slaHours = slaHours;
        this.description = description;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getSlaHours() { return slaHours; }
    public void setSlaHours(int slaHours) { this.slaHours = slaHours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
