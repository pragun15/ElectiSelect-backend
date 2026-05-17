package com.pragun.ElectiSelect.model;

public class SubjectUploadRowDTO {
    private String courseCode;
    private String title;
    private String department;
    private Integer maxSeats;
    private String restrictedDepts;
    private Integer credits;

    public SubjectUploadRowDTO() {}

    public SubjectUploadRowDTO(String courseCode,
                               String title,
                               String department,
                               Integer maxSeats,
                               String restrictedDepts,
                               Integer credits) {
        this.courseCode = courseCode;
        this.title = title;
        this.department = department;
        this.maxSeats = maxSeats;
        this.restrictedDepts = restrictedDepts;
        this.credits = credits;
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getMaxSeats() { return maxSeats; }
    public void setMaxSeats(Integer maxSeats) { this.maxSeats = maxSeats; }
    public String getRestrictedDepts() { return restrictedDepts; }
    public void setRestrictedDepts(String restrictedDepts) { this.restrictedDepts = restrictedDepts; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
}
