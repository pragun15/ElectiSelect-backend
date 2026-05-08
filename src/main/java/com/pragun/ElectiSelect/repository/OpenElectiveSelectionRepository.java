package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.OpenElectiveSelection;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenElectiveSelectionRepository extends JpaRepository<OpenElectiveSelection, Long> {

    // Step 7 of the workflow.md transaction flow:
    // Pre-insert duplicate check — (student_id, session_id)
    // Called AFTER the subject row lock is acquired.
    boolean existsByStudentAndSession(User student, Session session);

    // Used by GET /api/student/my-selection to return the student's current selection.
    // A student can have at most one open-elective selection (enforced by DB UNIQUE constraint).
    Optional<OpenElectiveSelection> findFirstByStudentOrderByIdDesc(User student);
}
