package com.pragun.ElectiSelect.model;

import java.util.List;

/**
 * Response DTO for GET /api/electives/dept.
 * Groups subjects under their category, matching the shape:
 *   { categoryId, categoryName, subjects: [...] }
 *
 * subjects field reuses SubjectDTO so seat counts and field names stay consistent
 * with the open-elective response.
 */
public class DeptCategoryDTO {

    private Long categoryId;
    private String categoryName;
    private List<SubjectDTO> subjects;

    public DeptCategoryDTO(DeptCategory category, List<SubjectDTO> subjects) {
        this.categoryId = category.getId();
        this.categoryName = category.getCategoryName();
        this.subjects = subjects;
    }

    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public List<SubjectDTO> getSubjects() { return subjects; }
}
