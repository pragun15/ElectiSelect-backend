package com.pragun.ElectiSelect.model;

import java.util.List;

/**
 * Aggregate analytics snapshot for System Admin.
 */
public class AdminAnalyticsDTO {
    private final long totalStudents;
    private final long eligibleStudents;
    private final long ineligibleStudents;
    private final long openElectiveParticipants;
    private final long deptElectiveParticipants;
    private final List<DepartmentCountDTO> departmentCounts;
    private final List<SemesterCountDTO> semesterCounts;
    private final List<PopularElectiveDTO> openElectivePopular;
    private final List<PopularElectiveDTO> deptElectivePopular;

    public AdminAnalyticsDTO(long totalStudents,
                             long eligibleStudents,
                             long ineligibleStudents,
                             long openElectiveParticipants,
                             long deptElectiveParticipants,
                             List<DepartmentCountDTO> departmentCounts,
                             List<SemesterCountDTO> semesterCounts,
                             List<PopularElectiveDTO> openElectivePopular,
                             List<PopularElectiveDTO> deptElectivePopular) {
        this.totalStudents = totalStudents;
        this.eligibleStudents = eligibleStudents;
        this.ineligibleStudents = ineligibleStudents;
        this.openElectiveParticipants = openElectiveParticipants;
        this.deptElectiveParticipants = deptElectiveParticipants;
        this.departmentCounts = departmentCounts;
        this.semesterCounts = semesterCounts;
        this.openElectivePopular = openElectivePopular;
        this.deptElectivePopular = deptElectivePopular;
    }

    public long getTotalStudents() { return totalStudents; }
    public long getEligibleStudents() { return eligibleStudents; }
    public long getIneligibleStudents() { return ineligibleStudents; }
    public long getOpenElectiveParticipants() { return openElectiveParticipants; }
    public long getDeptElectiveParticipants() { return deptElectiveParticipants; }
    public List<DepartmentCountDTO> getDepartmentCounts() { return departmentCounts; }
    public List<SemesterCountDTO> getSemesterCounts() { return semesterCounts; }
    public List<PopularElectiveDTO> getOpenElectivePopular() { return openElectivePopular; }
    public List<PopularElectiveDTO> getDeptElectivePopular() { return deptElectivePopular; }
}
