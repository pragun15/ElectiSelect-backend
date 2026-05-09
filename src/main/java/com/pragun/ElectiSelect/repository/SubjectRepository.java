package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.model.Subject;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.id = :id")
    Optional<Subject> findByIdWithLock(@Param("id") Long id);

    /**
     * Fetch subjects for a given semester in any active session, excluding deleted subjects.
     * Uses explicit JPQL to avoid Spring Data JPA Boolean wrapper comparison bugs.
     */
    @Query("SELECT s FROM Subject s WHERE s.session.isActive = true AND s.session.semester = :semester AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    List<Subject> findBySession_IsActiveTrueAndSession_SemesterAndIsDeletedFalse(@Param("semester") int semester);

    /**
     * Fetch subjects for a given semester and session type (OPEN / DEPT), excluding deleted.
     * Uses explicit JPQL to avoid Spring Data JPA Boolean wrapper comparison bugs.
     */
    @Query("SELECT s FROM Subject s WHERE s.session.isActive = true AND s.session.semester = :semester AND s.session.type = :type AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    List<Subject> findBySession_IsActiveTrueAndSession_SemesterAndSession_TypeAndIsDeletedFalse(
            @Param("semester") int semester,
            @Param("type") SessionType type);

    /**
     * Fetch all non-deleted subjects for a specific category, scoped to a given session.
     * Used by the dept-elective flow to load subjects per category.
     */
    @Query("SELECT s FROM Subject s WHERE s.category.id = :categoryId AND s.session.id = :sessionId AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    List<Subject> findByCategoryIdAndSessionIdAndNotDeleted(
            @Param("categoryId") Long categoryId,
            @Param("sessionId") Long sessionId);

    @Query("SELECT CASE WHEN COUNT(s.id) > 0 THEN true ELSE false END " +
            "FROM Subject s WHERE s.session.id = :sessionId " +
            "AND (s.isDeleted = false OR s.isDeleted IS NULL)")
    boolean existsNonDeletedBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT s.session.id as sessionId, COUNT(s.id) as count " +
            "FROM Subject s WHERE (s.isDeleted = false OR s.isDeleted IS NULL) " +
            "GROUP BY s.session.id")
    List<SessionSubjectCountProjection> countSubjectsBySession();

    interface SessionSubjectCountProjection {
        Long getSessionId();
        Long getCount();
    }
}