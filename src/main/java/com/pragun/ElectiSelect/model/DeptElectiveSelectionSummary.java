package com.pragun.ElectiSelect.model;

/**
 * Read-only summary of a submitted department elective selection.
 */
public class DeptElectiveSelectionSummary {
    private final Long categoryId;
    private final String categoryName;
    private final Long subjectId;
    private final String courseCode;
    private final String title;

    public DeptElectiveSelectionSummary(Long categoryId,
                                        String categoryName,
                                        Long subjectId,
                                        String courseCode,
                                        String title) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.subjectId = subjectId;
        this.courseCode = courseCode;
        this.title = title;
    }

    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Long getSubjectId() { return subjectId; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
}
