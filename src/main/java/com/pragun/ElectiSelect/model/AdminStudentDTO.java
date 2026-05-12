package com.pragun.ElectiSelect.model;

/**
 * Lightweight student row for System Admin Student Management.
 *
 * Truth source: app_users + academic_state.
 * Participation flags mean ANY selection exists for student (not session-scoped).
 */
public class AdminStudentDTO {
    private final Long id;
    private final String name;
    private final String email;
    private final String usn;
    private final String department;
    private final Integer semester;
    private final boolean eligible;
    private final Role role;
    private final boolean openElectiveSubmitted;
    private final boolean deptElectiveSubmitted;

    public AdminStudentDTO(Long id,
                           String name,
                           String email,
                           String usn,
                           String department,
                           Integer semester,
                           boolean eligible,
                           Role role,
                           boolean openElectiveSubmitted,
                           boolean deptElectiveSubmitted) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.usn = usn;
        this.department = department;
        this.semester = semester;
        this.eligible = eligible;
        this.role = role;
        this.openElectiveSubmitted = openElectiveSubmitted;
        this.deptElectiveSubmitted = deptElectiveSubmitted;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUsn() { return usn; }
    public String getDepartment() { return department; }
    public Integer getSemester() { return semester; }
    public boolean isEligible() { return eligible; }
    public Role getRole() { return role; }
    public boolean isOpenElectiveSubmitted() { return openElectiveSubmitted; }
    public boolean isDeptElectiveSubmitted() { return deptElectiveSubmitted; }
}
