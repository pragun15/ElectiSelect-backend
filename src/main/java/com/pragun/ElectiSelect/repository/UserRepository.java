package com.pragun.ElectiSelect.repository;

// 1. REMOVE THIS: import org.springframework.security.core.userdetails.User;

// 2. ADD YOUR MODEL IMPORT:
import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // This method will be used later for Google OAuth login logic
    Optional<User> findByEmail(String email);

    long countByRole(Role role);

    @Query("SELECT u.id as userId, u.name as name, u.usn as usn, u.department as department, " +
            "s.currentSemester as currentSemester, s.isEligible as eligible " +
            "FROM User u LEFT JOIN AcademicState s ON s.user = u " +
            "WHERE u.role = :role")
    List<StudentRowProjection> findStudentRows(@Param("role") Role role);

    interface StudentRowProjection {
        Long getUserId();
        String getName();
        String getUsn();
        String getDepartment();
        Integer getCurrentSemester();
        Boolean getEligible();
    }
}