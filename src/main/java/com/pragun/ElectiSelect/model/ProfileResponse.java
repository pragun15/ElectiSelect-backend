package com.pragun.ElectiSelect.model;

import java.time.LocalDateTime;

/**
 * Response body for GET /api/student/profile — workflow.md §6, §5 Session State Endpoint.
 * Contains: student identity, academic state, and active session (null if none).
 */
public class ProfileResponse {

    private final UserInfo user;
    private final AcademicStateInfo academicState;
    private final SessionInfo activeSession; // null when no active session for student's semester (kept for backward compat)
    private final SessionInfo openSession;   // active OPEN session for this semester, or null
    private final SessionInfo deptSession;   // active DEPARTMENT session for this semester, or null
    
    private final boolean openElectiveAvailable;
    private final boolean deptElectiveAvailable;

    public ProfileResponse(User user, AcademicState state, Session openSession, Session deptSession) {
        this.user = new UserInfo(user);
        this.academicState = new AcademicStateInfo(state);
        
        Session legacy = openSession != null ? openSession : deptSession;
        this.activeSession = legacy != null ? new SessionInfo(legacy) : null;
        this.openSession   = openSession != null ? new SessionInfo(openSession) : null;
        this.deptSession   = deptSession != null ? new SessionInfo(deptSession) : null;
        
        LocalDateTime now = LocalDateTime.now();
        this.openElectiveAvailable = state != null && state.isEligible() && openSession != null && 
                                     openSession.getIsActive() != null && openSession.getIsActive() &&
                                     openSession.getStartTime() != null && openSession.getEndTime() != null &&
                                     !now.isBefore(openSession.getStartTime()) && !now.isAfter(openSession.getEndTime());
                                     
        this.deptElectiveAvailable = state != null && state.isEligible() && deptSession != null && 
                                     deptSession.getIsActive() != null && deptSession.getIsActive() &&
                                     deptSession.getStartTime() != null && deptSession.getEndTime() != null &&
                                     !now.isBefore(deptSession.getStartTime()) && !now.isAfter(deptSession.getEndTime());
    }

    public UserInfo         getUser()          { return user; }
    public AcademicStateInfo getAcademicState() { return academicState; }
    public SessionInfo      getActiveSession() { return activeSession; }
    public SessionInfo      getOpenSession()   { return openSession; }
    public SessionInfo      getDeptSession()   { return deptSession; }
    public boolean          isOpenElectiveAvailable() { return openElectiveAvailable; }
    public boolean          isDeptElectiveAvailable() { return deptElectiveAvailable; }

    // ── Nested DTOs ────────────────────────────────────────────────────────────

    public static class UserInfo {
        private final String name;
        private final String email;
        private final String role;
        private final String department;

        UserInfo(User u) {
            this.name       = u.getName();
            this.email      = u.getEmail();
            this.role       = u.getRole() != null ? u.getRole().name() : null;
            this.department = u.getDepartment();
        }

        public String getName()       { return name; }
        public String getEmail()      { return email; }
        public String getRole()       { return role; }
        public String getDepartment() { return department; }
    }

    public static class AcademicStateInfo {
        private final int     currentSemester;
        private final boolean eligible;

        AcademicStateInfo(AcademicState s) {
            this.currentSemester = s.getCurrentSemester();
            this.eligible        = s.isEligible();
        }

        public int     getCurrentSemester() { return currentSemester; }
        public boolean isEligible()         { return eligible; }
    }

    public static class SessionInfo {
        private final String        type;
        private final int           semester;
        private final boolean       active;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        SessionInfo(Session s) {
            this.type      = s.getType() != null ? s.getType().name() : null;
            this.semester  = s.getSemester();
            this.active    = s.getIsActive() != null && s.getIsActive();
            this.startTime = s.getStartTime();
            this.endTime   = s.getEndTime();
        }

        public String        getType()      { return type; }
        public int           getSemester()  { return semester; }
        public boolean       isActive()     { return active; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime()   { return endTime; }
    }
}
