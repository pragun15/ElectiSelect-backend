package com.pragun.ElectiSelect.model;

public class SelectionDetailDTO {
    private boolean selected;
    private String courseCode;
    private String subjectName;
    private boolean sessionActive;

    public SelectionDetailDTO() {
    }

    public SelectionDetailDTO(boolean selected) {
        this.selected = selected;
    }

    public SelectionDetailDTO(boolean selected, String courseCode, String subjectName, boolean sessionActive) {
        this.selected = selected;
        this.courseCode = courseCode;
        this.subjectName = subjectName;
        this.sessionActive = sessionActive;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public void setSessionActive(boolean sessionActive) {
        this.sessionActive = sessionActive;
    }
}
