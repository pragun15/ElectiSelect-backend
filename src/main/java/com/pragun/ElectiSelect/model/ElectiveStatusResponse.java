package com.pragun.ElectiSelect.model;

/**
 * Lightweight truth status for elective visibility/submission.
 */
public class ElectiveStatusResponse {
    private final boolean visible;
    private final boolean eligible;
    private final boolean submitted;
    private final boolean locked;
    private final boolean semesterMatched;
    private final Long sessionId;
    private final String academicYear;
    private final java.time.LocalDateTime startTime;
    private final java.time.LocalDateTime endTime;
    private final boolean sessionActive;
    private final String sessionType;

    public ElectiveStatusResponse(boolean visible,
                                  boolean eligible,
                                  boolean submitted,
                                  boolean locked,
                                  boolean semesterMatched,
                                  Long sessionId,
                                  String academicYear,
                                  java.time.LocalDateTime startTime,
                                  java.time.LocalDateTime endTime,
                                  boolean sessionActive,
                                  String sessionType) {
        this.visible = visible;
        this.eligible = eligible;
        this.submitted = submitted;
        this.locked = locked;
        this.semesterMatched = semesterMatched;
        this.sessionId = sessionId;
        this.academicYear = academicYear;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sessionActive = sessionActive;
        this.sessionType = sessionType;
    }

    public boolean isVisible() { return visible; }
    public boolean isEligible() { return eligible; }
    public boolean isSubmitted() { return submitted; }
    public boolean isLocked() { return locked; }
    public boolean isSemesterMatched() { return semesterMatched; }
    public Long getSessionId() { return sessionId; }
    public String getAcademicYear() { return academicYear; }
    public java.time.LocalDateTime getStartTime() { return startTime; }
    public java.time.LocalDateTime getEndTime() { return endTime; }
    public boolean isSessionActive() { return sessionActive; }
    public String getSessionType() { return sessionType; }
}
