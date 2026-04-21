package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.Registration;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    // This helps us check if a student has already picked an elective
    // so they don't pick two!
    Optional<Registration> findByUser(User user);

    boolean existsByUser(User user);
}