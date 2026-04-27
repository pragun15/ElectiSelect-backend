package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RegistrationService {

    private final SubjectRepository subjectRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final AcademicStateRepository academicStateRepository;

    public RegistrationService(SubjectRepository subjectRepository,
                               RegistrationRepository registrationRepository,
                               UserRepository userRepository,
                               OpenElectiveSelectionRepository openElectiveSelectionRepository,
                               AcademicStateRepository academicStateRepository) {
        this.subjectRepository = subjectRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.academicStateRepository = academicStateRepository;
    }

    @Transactional
    public void registerStudentForElective(String email, Long subjectId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subject subject = subjectRepository.findByIdWithLock(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // ── Step 3: Check subject availability ──────────────────────────────────
        if (subject.isDeleted()) {
            throw new RuntimeException("SUBJECT_UNAVAILABLE");
        }

        // ── Step 2: Check seat availability ────────────────────────────────────
        if (subject.getFilled_seats() >= subject.getMaxSeats()) {
            throw new RuntimeException("NO_SEATS_AVAILABLE");
        }

        // ── Step 4: Apply department access rule inside transaction ─────────────
        // Re-applied here even though it was filtered at list-fetch time.
        // A student can bypass the frontend and POST directly with any subject_id.
        // allowed_departments (allowlist) takes precedence over restricted_departments.
        String allowedDepts = subject.getAllowedDepts();
        String restrictedDepts = subject.getRestrictedDepts();
        String studentDept = user.getDepartment();
        if (allowedDepts != null && !allowedDepts.isBlank()) {
            // Allowlist mode: ONLY these departments may select this subject.
            boolean isAllowed = Arrays.stream(allowedDepts.split(","))
                    .map(String::trim)
                    .anyMatch(dept -> dept.equals(studentDept));
            if (!isAllowed) {
                throw new RuntimeException("DEPARTMENT_RESTRICTED");
            }
        } else if (restrictedDepts != null && !restrictedDepts.isBlank()) {
            // Blocklist mode: these departments are explicitly excluded.
            boolean isRestricted = Arrays.stream(restrictedDepts.split(","))
                    .map(String::trim)
                    .anyMatch(dept -> dept.equals(studentDept));
            if (isRestricted) {
                throw new RuntimeException("DEPARTMENT_RESTRICTED");
            }
        }

        // ── Step 5: Re-validate session state INSIDE the transaction ────────────
        // The session may have been deactivated or expired between the time the
        // student fetched the subject list (GET) and submitted this selection (POST).
        // This check must happen while the subject row lock is held.
        Session session = subject.getSession();
        if (!session.isActive()
                || LocalDateTime.now().isBefore(session.getStartTime())
                || LocalDateTime.now().isAfter(session.getEndTime())) {
            throw new RuntimeException("SESSION_INVALID");
        }
        // Semester must match inside the transaction — admin may have changed the
        // student's semester between the GET (subject list) and this POST (submit).
        AcademicState academicState = academicStateRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Academic state not found"));
        // Eligibility is a hard block regardless of session state — workflow.md Business Rule 6.
        // Read live from DB: admin may have marked the student ineligible after login.
        if (!academicState.isEligible()) {
            throw new RuntimeException("NOT_ELIGIBLE");
        }
        if (academicState.getCurrentSemester() != session.getSemester()) {
            throw new RuntimeException("SESSION_INVALID");
        }

        // ── Step 7: Pre-insert duplicate check by (student_id, session_id) ─────
        // Scoped to this session — a student may only hold one open elective
        // selection per session. This check comes AFTER the lock so that two
        // concurrent requests cannot both pass it simultaneously.
        // The DB UNIQUE constraint on (student_id, session_id) is the final guard
        // and must not be removed even though this pre-check exists.
        if (openElectiveSelectionRepository.existsByStudentAndSession(user, session)) {
            throw new RuntimeException("ALREADY_SELECTED");
        }

        // ── Step 6: Claim the seat BEFORE inserting the selection record ────────
        // If the subsequent save() fails, the entire transaction rolls back and
        // filled_seats reverts — no phantom seat loss is possible.
        subject.setFilled_seats(subject.getFilled_seats() + 1);
        subjectRepository.save(subject);

        // ── Step 8: Insert the selection record ────────────────────────────────
        // The DB UNIQUE constraint is the final guard against any duplicate that
        // slips past the pre-check (e.g. two requests passing step 7 at the exact
        // same instant). A constraint violation here rolls back the whole transaction.
        OpenElectiveSelection selection = new OpenElectiveSelection();
        selection.setStudent(user);
        selection.setSubject(subject);
        selection.setSession(session);
        openElectiveSelectionRepository.save(selection);
    }

    public List<Registration> getAllRegistrations() {
        return registrationRepository.findAll();
    }

    public boolean hasStudentAlreadyRegistered(User user) {
        return registrationRepository.existsByUser(user);
    }
}