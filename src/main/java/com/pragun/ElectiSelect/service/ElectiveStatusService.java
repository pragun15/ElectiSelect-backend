package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.DeptElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.OpenElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.SessionRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ElectiveStatusService {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;
    private final SessionRepository sessionRepository;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;

    public ElectiveStatusService(UserRepository userRepository,
                                 AcademicStateRepository academicStateRepository,
                                 SessionRepository sessionRepository,
                                 OpenElectiveSelectionRepository openElectiveSelectionRepository,
                                 DeptElectiveSelectionRepository deptElectiveSelectionRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
        this.sessionRepository = sessionRepository;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
    }

    public ElectiveStatusResponse getOpenStatus(String email) {
        return buildStatus(email, SessionType.OPEN);
    }

    public ElectiveStatusResponse getDeptStatus(String email) {
        return buildStatus(email, SessionType.DEPARTMENT);
    }

    private ElectiveStatusResponse buildStatus(String email, SessionType type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: User not found."));
        AcademicState state = academicStateRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: Academic state not found."));

        boolean eligible = state.isEligible();
        int semester = state.getCurrentSemester();

    Session activeForSemester = sessionRepository
        .findByIsActiveTrueAndSemesterAndType(semester, type)
        .stream()
        .findFirst()
        .orElse(null);

    Session semesterSession = sessionRepository.findTopBySemesterAndTypeOrderByIdDesc(semester, type);

    boolean semesterMatched = semesterSession != null;
    Session session = activeForSemester != null ? activeForSemester : semesterSession;

        boolean visible = false;
        if (eligible && activeForSemester != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = activeForSemester.getStartTime();
            LocalDateTime end = activeForSemester.getEndTime();
            boolean active = activeForSemester.getIsActive() != null && activeForSemester.getIsActive();
            boolean inWindow = start != null && end != null && !now.isBefore(start) && !now.isAfter(end);
            visible = active && inWindow;
        }

        boolean submitted = false;
        if (session != null) {
            if (type == SessionType.OPEN) {
                submitted = openElectiveSelectionRepository.existsByStudentAndSession(user, session);
            } else {
                submitted = deptElectiveSelectionRepository.existsByStudentAndSession(user, session);
            }
        }

        return new ElectiveStatusResponse(
                visible,
                eligible,
                submitted,
                submitted,
                semesterMatched,
                session != null ? session.getId() : null,
                activeForSemester != null ? activeForSemester.getAcademicYear() : null,
                activeForSemester != null ? activeForSemester.getStartTime() : null,
                activeForSemester != null ? activeForSemester.getEndTime() : null,
                activeForSemester != null,
                activeForSemester != null && activeForSemester.getType() != null ? activeForSemester.getType().name() : (session != null && session.getType() != null ? session.getType().name() : null)
        );
    }
}
