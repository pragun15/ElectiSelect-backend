package com.pragun.ElectiSelect.model;

/**
 * Read-only popular elective summary for SUPER_ADMIN dashboard.
 */
public class PopularElectiveDTO {
    private final Long subjectId;
    private final String courseCode;
    private final String title;
    private final long selectionCount;
    private final int filledSeats;
    private final int maxSeats;
    private final Integer credits;

    public PopularElectiveDTO(Long subjectId,
                              String courseCode,
                              String title,
                              long selectionCount,
                              int filledSeats,
                              int maxSeats,
                              Integer credits) {
        this.subjectId = subjectId;
        this.courseCode = courseCode;
        this.title = title;
        this.selectionCount = selectionCount;
        this.filledSeats = filledSeats;
        this.maxSeats = maxSeats;
        this.credits = credits;
    }

    public Long getSubjectId() { return subjectId; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public long getSelectionCount() { return selectionCount; }
    public int getFilledSeats() { return filledSeats; }
    public int getMaxSeats() { return maxSeats; }
    public Integer getCredits() { return credits; }
}
