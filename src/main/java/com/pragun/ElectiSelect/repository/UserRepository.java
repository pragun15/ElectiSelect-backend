package com.pragun.ElectiSelect.repository;

// 1. REMOVE THIS: import org.springframework.security.core.userdetails.User;

// 2. ADD YOUR MODEL IMPORT:
import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.AdminStudentDTO;
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

    boolean existsByEmail(String email);
    boolean existsByUsn(String usn);

    long countByRole(Role role);

    @Query("SELECT u.department as department, COUNT(u.id) as count " +
        "FROM User u WHERE u.role = :role GROUP BY u.department")
    List<DepartmentCountProjection> countStudentsByDepartment(@Param("role") Role role);

    @Query("SELECT u.id as userId, u.name as name, u.usn as usn, u.department as department, " +
            "s.currentSemester as currentSemester, s.isEligible as eligible " +
            "FROM User u LEFT JOIN AcademicState s ON s.user = u " +
            "WHERE u.role = :role")
    List<StudentRowProjection> findStudentRows(@Param("role") Role role);

    // Student Management module (System Admin):
    // - Truth: app_users + academic_state
    // - Participation flags: ANY selection exists for student (not session-scoped)
    // - Supports search + filters without loading entity graphs
    // - Semester: null if academic_state missing (not 0)
    @Query("SELECT new com.pragun.ElectiSelect.model.AdminStudentDTO(" +
        "u.id, u.name, u.email, u.usn, u.department, " +
        "CASE WHEN s IS NULL THEN NULL ELSE s.currentSemester END, COALESCE(s.isEligible, false), u.role, " +
        "CASE WHEN EXISTS(SELECT 1 FROM OpenElectiveSelection oe WHERE oe.student = u) THEN true ELSE false END, " +
        "CASE WHEN EXISTS(SELECT 1 FROM DeptElectiveSelection de WHERE de.student = u) THEN true ELSE false END" +
        ") " +
        "FROM User u " +
        "LEFT JOIN AcademicState s ON s.user = u " +
        "WHERE u.role = com.pragun.ElectiSelect.model.Role.STUDENT " +
        "AND (:search IS NULL OR :search = '' OR " +
        "     LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
        "     LOWER(u.usn) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "AND (:department IS NULL OR :department = '' OR u.department = :department) " +
        "AND (:semester IS NULL OR s.currentSemester = :semester) " +
        "AND (:eligible IS NULL OR s.isEligible = :eligible)")
    List<AdminStudentDTO> findAdminStudents(
        @Param("search") String search,
        @Param("department") String department,
        @Param("semester") Integer semester,
        @Param("eligible") Boolean eligible);

    interface StudentRowProjection {
        Long getUserId();
        String getName();
        String getUsn();
        String getDepartment();
        Integer getCurrentSemester();
        Boolean getEligible();
    }

    interface DepartmentCountProjection {
        String getDepartment();
        Long getCount();
    }
}