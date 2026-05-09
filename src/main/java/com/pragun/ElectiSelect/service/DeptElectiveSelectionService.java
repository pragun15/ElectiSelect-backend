package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeptElectiveSelectionService {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;
    private final SessionRepository sessionRepository;
    private final DeptCategoryRepository deptCategoryRepository;
    private final SubjectRepository subjectRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;

    public DeptElectiveSelectionService(UserRepository userRepository,
                                        AcademicStateRepository academicStateRepository,
                                        SessionRepository sessionRepository,
                                        DeptCategoryRepository deptCategoryRepository,
                                        SubjectRepository subjectRepository,
                                        DeptElectiveSelectionRepository deptElectiveSelectionRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
        this.sessionRepository = sessionRepository;
        this.deptCategoryRepository = deptCategoryRepository;
        this.subjectRepository = subjectRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
    }

    @Transactional(readOnly = true)
    public DeptElectiveSelectionStatusResponse getMySelections(String email) {
    User student = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: User not found."));

    AcademicState academicState = academicStateRepository.findByUser(student)
        .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: Academic state not found."));

    int semester = academicState.getCurrentSemester();
    List<Session> deptSessions = sessionRepository.findByIsActiveTrueAndSemesterAndType(semester, SessionType.DEPARTMENT);
    if (deptSessions.isEmpty()) {
        return new DeptElectiveSelectionStatusResponse(false, null, List.of());
    }

    Session session = deptSessions.get(0);
    List<DeptElectiveSelection> selections = deptElectiveSelectionRepository
        .findByStudentAndSessionWithDetails(student, session);

    if (selections.isEmpty()) {
        return new DeptElectiveSelectionStatusResponse(false, session.getId(), List.of());
    }

    List<DeptElectiveSelectionSummary> summaries = selections.stream()
        .map(sel -> new DeptElectiveSelectionSummary(
            sel.getCategory().getId(),
            sel.getCategory().getCategoryName(),
            sel.getSubject().getId(),
            sel.getSubject().getCourseCode(),
            sel.getSubject().getTitle()))
        .collect(java.util.stream.Collectors.toList());

    return new DeptElectiveSelectionStatusResponse(true, session.getId(), summaries);
    }

    @Transactional
    public void submitSelections(String email, List<DeptElectiveSelectionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new RuntimeException("VALIDATION_FAILED: Submission cannot be empty.");
        }

        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: User not found."));
        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("VALIDATION_FAILED: Only students can submit department electives.");
        }

        AcademicState academicState = academicStateRepository.findByUser(student)
                .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: Academic state not found."));
        if (!academicState.isEligible()) {
            throw new RuntimeException("NOT_ELIGIBLE");
        }

        int semester = academicState.getCurrentSemester();
        List<Session> deptSessions = sessionRepository.findByIsActiveTrueAndSemesterAndType(semester, SessionType.DEPARTMENT);
        if (deptSessions.isEmpty()) {
            throw new RuntimeException("SESSION_INVALID");
        }

        Session session = deptSessions.get(0);
        if (session.getIsActive() == null || !session.getIsActive()) {
            throw new RuntimeException("SESSION_INVALID");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new RuntimeException("SESSION_INVALID");
        }

        if (deptElectiveSelectionRepository.existsByStudentAndSession(student, session)) {
            throw new RuntimeException("ALREADY_SELECTED");
        }

        List<DeptCategory> categories = deptCategoryRepository.findBySession_Id(session.getId());
        if (categories.isEmpty()) {
            throw new RuntimeException("VALIDATION_FAILED: No categories configured for this session.");
        }

        Map<Long, DeptCategory> categoryById = categories.stream()
                .collect(Collectors.toMap(DeptCategory::getId, c -> c));
        Set<Long> expectedCategoryIds = categoryById.keySet();

        Set<Long> submittedCategoryIds = new HashSet<>();
        for (DeptElectiveSelectionRequest req : requests) {
            if (req.getCategoryId() == null || req.getSubjectId() == null) {
                throw new RuntimeException("VALIDATION_FAILED: categoryId and subjectId are required.");
            }
            if (!submittedCategoryIds.add(req.getCategoryId())) {
                throw new RuntimeException("VALIDATION_FAILED: Duplicate category submissions are not allowed.");
            }
        }

        if (!submittedCategoryIds.equals(expectedCategoryIds)) {
            throw new RuntimeException("VALIDATION_FAILED: All categories must be submitted exactly once.");
        }

        List<DeptElectiveSelection> selections = new ArrayList<>();
        for (DeptElectiveSelectionRequest req : requests) {
            DeptCategory category = categoryById.get(req.getCategoryId());
            if (category == null) {
                throw new RuntimeException("VALIDATION_FAILED: Invalid category for this session.");
            }

            Subject subject = subjectRepository.findById(req.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("VALIDATION_FAILED: Subject not found."));

            if (subject.getSession() == null || !session.getId().equals(subject.getSession().getId())) {
                throw new RuntimeException("VALIDATION_FAILED: Subject does not belong to active session.");
            }
            if (subject.getCategory() == null || !category.getId().equals(subject.getCategory().getId())) {
                throw new RuntimeException("VALIDATION_FAILED: Subject does not belong to the selected category.");
            }
            boolean isSubjectDeleted = (subject.getIsDeleted() != null && subject.getIsDeleted())
                    || (subject.getDeleted() != null && subject.getDeleted());
            if (isSubjectDeleted) {
                throw new RuntimeException("SUBJECT_UNAVAILABLE");
            }

            DeptElectiveSelection selection = new DeptElectiveSelection();
            selection.setStudent(student);
            selection.setSession(session);
            selection.setCategory(category);
            selection.setSubject(subject);
            selection.setSelectedAt(LocalDateTime.now());
            selections.add(selection);
        }

        try {
            deptElectiveSelectionRepository.saveAll(selections);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("ALREADY_SELECTED");
        }
    }
}
