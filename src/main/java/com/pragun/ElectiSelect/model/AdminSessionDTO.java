package com.pragun.ElectiSelect.model;

import java.time.LocalDateTime;

/**
 * Read-only session overview for SUPER_ADMIN dashboard.
 */
public class AdminSessionDTO {
    private final Long id;
    private final String type;
    private final int semester;
    private final String academicYear;
    private final boolean active;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long subjectCount;

    public AdminSessionDTO(Session session, long subjectCount) {
        this.id = session.getId();
        this.type = session.getType() != null ? session.getType().name() : null;
        this.semester = session.getSemester();
        this.academicYear = session.getAcademicYear();
        this.active = session.getIsActive() != null && session.getIsActive();
        this.startTime = session.getStartTime();
        this.endTime = session.getEndTime();
        this.subjectCount = subjectCount;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public int getSemester() { return semester; }
    public String getAcademicYear() { return academicYear; }
    @com.fasterxml.jackson.annotation.JsonProperty("active")
    public boolean isActive() { return active; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public long getSubjectCount() { return subjectCount; }
}
