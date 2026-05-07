package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session createSession(Session session) {
        // ⛔ NON-NEGOTIABLE: Only ONE active session per type globally (workflow.md §5, BR-7).
        // Scope is type-only — NOT type+semester. Two active OPEN sessions for different
        // semesters are prohibited.
        if (session.getIsActive() != null && session.getIsActive()) {
            List<Session> activeSessions = sessionRepository.findByIsActiveTrueAndType(session.getType());
            if (!activeSessions.isEmpty()) {
                throw new RuntimeException("SESSION_ALREADY_ACTIVE");
            }
        }
        return sessionRepository.save(session);
    }

    public List<Session> getAllSessions() {
        return sessionRepository.findAll();
    }

    @Transactional
    public void toggleSessionStatus(Long sessionId, boolean status) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Activation path: enforce the same global uniqueness check as createSession().
        // Without this, an admin can bypass the creation-time guard by toggling an
        // existing inactive session — the most likely bypass vector.
        if (status) {
            List<Session> activeSessions = sessionRepository.findByIsActiveTrueAndType(session.getType());
            // Exclude the session being toggled itself in case it's already active
            boolean anotherIsActive = activeSessions.stream()
                    .anyMatch(s -> !s.getId().equals(sessionId));
            if (anotherIsActive) {
                throw new RuntimeException("SESSION_ALREADY_ACTIVE");
            }
        }

        session.setIsActive(status);
        sessionRepository.save(session);
    }
}