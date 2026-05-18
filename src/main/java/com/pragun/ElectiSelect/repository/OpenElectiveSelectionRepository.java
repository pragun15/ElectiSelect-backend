package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.OpenElectiveSelection;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface OpenElectiveSelectionRepository extends JpaRepository<OpenElectiveSelection, Long> {

    // Step 7 of the workflow.md transaction flow:
    // Pre-insert duplicate check — (student_id, session_id)
    // Called AFTER the subject row lock is acquired.
    boolean existsByStudentAndSession(User student, Session session);

    // Used by GET /api/student/my-selection to return the student's current selection.
    // A student can have at most one open-elective selection (enforced by DB UNIQUE constraint).
    Optional<OpenElectiveSelection> findFirstByStudentOrderByIdDesc(User student);

    List<OpenElectiveSelection> findBySessionId(Long sessionId);

    // Student Management (admin analytics): any open elective submission exists for student
    boolean existsByStudent_Id(Long studentId);

    @Query("SELECT COUNT(DISTINCT s.student.id) FROM OpenElectiveSelection s JOIN s.session sess " +
           "WHERE (:sessionIds IS NULL OR sess.id IN :sessionIds)")
    long countDistinctStudentsFiltered(
            @org.springframework.data.repository.query.Param("sessionIds") List<Long> sessionIds);

    @Query("SELECT s.student.id as studentId, s.session.id as sessionId, COUNT(s.id) as count " +
            "FROM OpenElectiveSelection s GROUP BY s.student.id, s.session.id")
    List<SelectionCountProjection> countByStudentAndSession();

    @Query("SELECT subj.id as subjectId, subj.courseCode as courseCode, subj.title as title, " +
        "COUNT(sel.id) as selectionCount, subj.filled_seats as filledSeats, subj.maxSeats as maxSeats, " +
        "subj.credits as credits " +
        "FROM OpenElectiveSelection sel JOIN sel.subject subj JOIN sel.session sess " +
        "WHERE (subj.isDeleted = false OR subj.isDeleted IS NULL) " +
        "AND (:sessionIds IS NULL OR sess.id IN :sessionIds) " +
        "GROUP BY subj.id, subj.courseCode, subj.title, subj.filled_seats, subj.maxSeats, subj.credits " +
        "ORDER BY COUNT(sel.id) DESC")
    List<PopularElectiveProjection> findPopularOpenElectivesFiltered(
            @org.springframework.data.repository.query.Param("sessionIds") List<Long> sessionIds,
            Pageable pageable);

    interface PopularElectiveProjection {
        Long getSubjectId();
        String getCourseCode();
        String getTitle();
        Long getSelectionCount();
        Integer getFilledSeats();
        Integer getMaxSeats();
        Integer getCredits();
    }

    interface StaffExportProjection {
        String getStudentName();
        String getUsn();
        String getDepartment();
        String getSubjectTitle();
        String getCourseCode();
        String getCategoryName(); // For compatibility with Dept
    }

    @Query("SELECT sel.student.name as studentName, sel.student.usn as usn, sel.student.department as department, " +
           "sel.subject.title as subjectTitle, sel.subject.courseCode as courseCode " +
           "FROM OpenElectiveSelection sel WHERE sel.session.id = :sessionId")
    List<StaffExportProjection> findExportDataBySessionId(@org.springframework.data.repository.query.Param("sessionId") Long sessionId);
}
