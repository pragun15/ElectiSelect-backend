package com.pragun.ElectiSelect.model;

/**
 * Read-only student row for SUPER_ADMIN dashboard table.
 */
public class AdminStudentRowDTO {
    private final Long userId;
    private final String name;
    private final String usn;
    private final String department;
    private final Integer semester;
    private final boolean eligible;
    private final boolean openElectiveSelected;
    private final boolean deptElectiveCompleted;

    public AdminStudentRowDTO(Long userId,
                              String name,
                              String usn,
                              String department,
                              Integer semester,
                              boolean eligible,
                              boolean openElectiveSelected,
                              boolean deptElectiveCompleted) {
        this.userId = userId;
        this.name = name;
        this.usn = usn;
        this.department = department;
        this.semester = semester;
        this.eligible = eligible;
        this.openElectiveSelected = openElectiveSelected;
        this.deptElectiveCompleted = deptElectiveCompleted;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getUsn() { return usn; }
    public String getDepartment() { return department; }
    public Integer getSemester() { return semester; }
    public boolean isEligible() { return eligible; }
    public boolean isOpenElectiveSelected() { return openElectiveSelected; }
    public boolean isDeptElectiveCompleted() { return deptElectiveCompleted; }
}
