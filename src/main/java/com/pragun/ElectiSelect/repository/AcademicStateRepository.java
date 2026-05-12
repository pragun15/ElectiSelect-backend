package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface AcademicStateRepository extends JpaRepository<AcademicState, Long> {
    // Find eligibility and semester by the User's ID
    Optional<AcademicState> findByUser(User user);

    long countByCurrentSemester(int currentSemester);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AcademicState s SET s.currentSemester = s.currentSemester + 1 " +
           "WHERE s.currentSemester = :semester")
    int bulkPromoteSemester(@Param("semester") int semester);


}
