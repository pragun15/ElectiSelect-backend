package com.pragun.ElectiSelect.repository;

// REMOVE this: import org.hibernate.Session;
// ADD your actual entity import:
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByIsActiveTrueAndSemesterAndType(int semester, SessionType type);
    List<Session> findByIsActiveTrueAndType(SessionType type);
    List<Session> findByIsActiveTrueAndSemester(int semester);

    boolean existsByTypeAndSemesterAndAcademicYear(SessionType type, int semester, String academicYear);

    Session findTopBySemesterAndTypeOrderByIdDesc(int semester, SessionType type);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s FROM Session s " +
        "WHERE (:type IS NULL OR s.type = :type) " +
        "AND (:semester IS NULL OR s.semester = :semester) " +
        "AND (:academicYear IS NULL OR s.academicYear = :academicYear)")
    List<Session> findFilteredSessions(
        @org.springframework.data.repository.query.Param("type") SessionType type,
        @org.springframework.data.repository.query.Param("semester") Integer semester,
        @org.springframework.data.repository.query.Param("academicYear") String academicYear);
}