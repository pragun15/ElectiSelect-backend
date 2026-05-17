package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicStateRepository extends JpaRepository<AcademicState, Long> {
    // Find eligibility and semester by the User's ID
    Optional<AcademicState> findByUser(User user);

    long countByCurrentSemester(int currentSemester);

    @Query("SELECT COUNT(s.userId) FROM AcademicState s JOIN s.user u " +
        "WHERE u.role = com.pragun.ElectiSelect.model.Role.STUDENT " +
        "AND (:isEligible IS NULL OR s.isEligible = :isEligible) " +
        "AND (:semesters IS NULL OR s.currentSemester IN :semesters)")
    long countStudentsFiltered(@Param("isEligible") Boolean isEligible, @Param("semesters") List<Integer> semesters);

    @Query("SELECT s.currentSemester as semester, COUNT(s.userId) as count " +
        "FROM AcademicState s JOIN s.user u " +
        "WHERE u.role = com.pragun.ElectiSelect.model.Role.STUDENT " +
        "AND (:semesters IS NULL OR s.currentSemester IN :semesters) " +
        "GROUP BY s.currentSemester")
    List<SemesterCountProjection> countStudentsBySemesterFiltered(@Param("semesters") List<Integer> semesters);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AcademicState s SET s.currentSemester = s.currentSemester + 1 " +
           "WHERE s.currentSemester = :semester")
    int bulkPromoteSemester(@Param("semester") int semester);

    interface SemesterCountProjection {
        Integer getSemester();
        Long getCount();
    }


}
