package com.pragun.ElectiSelect.model;

/**
 * Request DTO for department elective submission.
 * Each entry maps a category to a chosen subject.
 */
public class DeptElectiveSelectionRequest {
    private Long categoryId;
    private Long subjectId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }
}
