package com.pragun.ElectiSelect.model;

import java.time.LocalDateTime;

/**
 * Response body for GET /api/student/profile — workflow.md §6, §5 Session State Endpoint.
 * Contains: student identity, academic state, and active session (null if none).
 */
public class ProfileResponse {

    private final UserInfo user;
    private final AcademicStateInfo academicState;
    private final SessionInfo activeSession; // null when no active session for student's semester

    public ProfileResponse(User user, AcademicState state, Session session) {
        this.user = new UserInfo(user);
        this.academicState = new AcademicStateInfo(state);
        this.activeSession = session != null ? new SessionInfo(session) : null;
    }

    public UserInfo         getUser()          { return user; }
    public AcademicStateInfo getAcademicState() { return academicState; }
    public SessionInfo      getActiveSession() { return activeSession; }

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
            this.active    = s.isActive();
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
