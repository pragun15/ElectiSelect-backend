package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.repository.SessionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;


    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }


    public Session createSession(Session session) {
        // Business Rule: Ensure only one session of a specific type is active
        // for a specific semester at a time to avoid student confusion.
        if (session.isActive()) {
            List<Session> activeSessions = sessionRepository.findByIsActiveTrueAndSemesterAndType(
                    session.getSemester(), session.getType());
            if (!activeSessions.isEmpty()) {
                throw new RuntimeException("An active session already exists for this semester and type.");
            }
        }
        return sessionRepository.save(session);
    }

    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    public void toggleSessionStatus(Long sessionId, boolean status) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        session.setActive(status);
        sessionRepository.save(session);
    }
}