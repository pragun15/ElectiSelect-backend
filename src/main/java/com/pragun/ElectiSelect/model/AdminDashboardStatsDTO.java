package com.pragun.ElectiSelect.model;

/**
 * Read-only summary stats for SUPER_ADMIN dashboard.
 */
public class AdminDashboardStatsDTO {
    private final long registeredStudents;
    private final long openElectiveTaken;
    private final long deptElectiveTaken;
    private final long fullyCompleted;

    public AdminDashboardStatsDTO(long registeredStudents,
                                  long openElectiveTaken,
                                  long deptElectiveTaken,
                                  long fullyCompleted) {
        this.registeredStudents = registeredStudents;
        this.openElectiveTaken = openElectiveTaken;
        this.deptElectiveTaken = deptElectiveTaken;
        this.fullyCompleted = fullyCompleted;
    }

    public long getRegisteredStudents() { return registeredStudents; }
    public long getOpenElectiveTaken() { return openElectiveTaken; }
    public long getDeptElectiveTaken() { return deptElectiveTaken; }
    public long getFullyCompleted() { return fullyCompleted; }
}
