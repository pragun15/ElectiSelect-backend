package com.pragun.ElectiSelect.model;

/**
 * Count of students grouped by department.
 */
public class DepartmentCountDTO {
    private final String department;
    private final long count;

    public DepartmentCountDTO(String department, long count) {
        this.department = department;
        this.count = count;
    }

    public String getDepartment() { return department; }
    public long getCount() { return count; }
}
