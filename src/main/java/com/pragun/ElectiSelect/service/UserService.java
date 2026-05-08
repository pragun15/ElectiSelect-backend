package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.ProfileResponse;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.SessionRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;
    private final SessionRepository sessionRepository;

    public UserService(UserRepository userRepository,
                       AcademicStateRepository academicStateRepository,
                       SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
        this.sessionRepository = sessionRepository;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public int getStudentSemester(String email) {
        User user = getUserByEmail(email);
        AcademicState state = academicStateRepository.findByUser(user)
                .orElseGet(() -> {
                    AcademicState newState = new AcademicState();
                    newState.setUser(user);
                    newState.setCurrentSemester(0);
                    return academicStateRepository.save(newState);
                });
        return state.getCurrentSemester();
    }

    /**
     * Builds the full profile response for GET /api/student/profile.
     * Loads User, AcademicState, and the active sessions for the student's semester.
     * Returns null for openSession / deptSession if none is active — frontend renders locked state.
     */
    public ProfileResponse getStudentProfile(String email) {
        User user = getUserByEmail(email);
        AcademicState state = academicStateRepository.findByUser(user)
                .orElseGet(() -> {
                    AcademicState newState = new AcademicState();
                    newState.setUser(user);
                    newState.setCurrentSemester(0);
                    return academicStateRepository.save(newState);
                });

        int semester = state.getCurrentSemester();

        // Fetch OPEN and DEPARTMENT sessions independently so Dashboard can show correct status per type
        List<Session> openSessions = sessionRepository.findByIsActiveTrueAndSemesterAndType(semester, SessionType.OPEN);
        List<Session> deptSessions = sessionRepository.findByIsActiveTrueAndSemesterAndType(semester, SessionType.DEPARTMENT);

        Session openSession = openSessions.isEmpty() ? null : openSessions.get(0);
        Session deptSession = deptSessions.isEmpty() ? null : deptSessions.get(0);

        return new ProfileResponse(user, state, openSession, deptSession);
    }
}