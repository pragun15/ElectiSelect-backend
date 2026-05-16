package com.pragun.ElectiSelect.model;

/**
 * Count of students grouped by semester.
 */
public class SemesterCountDTO {
    private final int semester;
    private final long count;

    public SemesterCountDTO(int semester, long count) {
        this.semester = semester;
        this.count = count;
    }

    public int getSemester() { return semester; }
    public long getCount() { return count; }
}
