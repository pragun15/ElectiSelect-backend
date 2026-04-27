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
}