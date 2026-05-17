package com.pragun.ElectiSelect.model;

public class SubjectUploadErrorDTO {
    private Integer rowNumber;
    private String courseCode;
    private String title;
    private String reason;

    public SubjectUploadErrorDTO() {}

    public SubjectUploadErrorDTO(Integer rowNumber, String courseCode, String title, String reason) {
        this.rowNumber = rowNumber;
        this.courseCode = courseCode;
        this.title = title;
        this.reason = reason;
    }

    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
