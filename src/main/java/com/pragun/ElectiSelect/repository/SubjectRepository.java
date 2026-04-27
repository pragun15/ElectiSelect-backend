package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.Subject;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subject s WHERE s.id = :id")
    Optional<Subject> findByIdWithLock(Long id);

    List<Subject> findBySession_IsActiveTrueAndSession_SemesterAndIsDeletedFalse(int semester);

    List<Subject> findBySession_IsActiveTrueAndSession_SemesterAndSession_TypeAndIsDeletedFalse(int semester,
            com.pragun.ElectiSelect.model.SessionType type);
}