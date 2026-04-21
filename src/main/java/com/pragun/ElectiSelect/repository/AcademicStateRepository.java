package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface AcademicStateRepository extends JpaRepository<AcademicState, Long> {
    // Find eligibility and semester by the User's ID
    Optional<AcademicState> findByUser(User user);


}
