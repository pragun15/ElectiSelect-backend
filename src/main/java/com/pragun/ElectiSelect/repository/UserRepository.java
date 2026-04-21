package com.pragun.ElectiSelect.repository;

// 1. REMOVE THIS: import org.springframework.security.core.userdetails.User;

// 2. ADD YOUR MODEL IMPORT:
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // This method will be used later for Google OAuth login logic
    Optional<User> findByEmail(String email);
}