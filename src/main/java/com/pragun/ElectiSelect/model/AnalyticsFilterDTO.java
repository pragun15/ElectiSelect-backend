package com.pragun.ElectiSelect.model;

public class AnalyticsFilterDTO {
    private String type;
    private Integer semester;
    private String academicYear;
    private Long sessionId;

    public AnalyticsFilterDTO() {
    }

    public AnalyticsFilterDTO(String type, Integer semester, String academicYear, Long sessionId) {
        this.type = type;
        this.semester = semester;
        this.academicYear = academicYear;
        this.sessionId = sessionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isEmpty() {
        return type == null && semester == null && academicYear == null && sessionId == null;
    }
}
