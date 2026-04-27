package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.ProfileResponse;
import com.pragun.ElectiSelect.model.Session;
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
                .orElseThrow(() -> new RuntimeException("Academic state not found for user: " + email));
        return state.getCurrentSemester();
    }

    /**
     * Builds the full profile response for GET /api/student/profile.
     * Loads User, AcademicState, and the active session (if any) for the student's semester.
     * Returns null for activeSession if no session is currently active — frontend renders locked state.
     */
    public ProfileResponse getStudentProfile(String email) {
        User user = getUserByEmail(email);
        AcademicState state = academicStateRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Academic state not found for user: " + email));

        // Find any active session (OPEN or DEPARTMENT) matching the student's current semester
        List<Session> activeSessions = sessionRepository.findByIsActiveTrueAndSemester(state.getCurrentSemester());
        Session activeSession = activeSessions.isEmpty() ? null : activeSessions.get(0);

        return new ProfileResponse(user, state, activeSession);
    }
}