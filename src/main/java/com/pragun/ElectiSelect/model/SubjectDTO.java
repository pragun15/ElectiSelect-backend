package com.pragun.ElectiSelect.model;

/**
 * Read-only response object returned to students for the open elective subject list.
 * Includes remainingSeats computed at query time — workflow.md §9 rule 4.
 * The Subject entity itself is never sent directly so that internal fields
 * (filled_seats, session FK, etc.) are not exposed to the client.
 */
public class SubjectDTO {

    private Long id;
    private String courseCode;
    private String title;
    private String department;
    private int maxSeats;
    private int filledSeats;
    private int remainingSeats; // maxSeats - filledSeats, computed at list time

    public SubjectDTO(Subject subject) {
        this.id = subject.getId();
        this.courseCode = subject.getCourseCode();
        this.title = subject.getTitle();
        this.department = subject.getDepartment();
        this.maxSeats = subject.getMaxSeats();
        this.filledSeats = subject.getFilled_seats();
        this.remainingSeats = subject.getMaxSeats() - subject.getFilled_seats();
    }

    public Long getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public String getDepartment() { return department; }
    public int getMaxSeats() { return maxSeats; }
    public int getFilledSeats() { return filledSeats; }
    public int getRemainingSeats() { return remainingSeats; }
}
