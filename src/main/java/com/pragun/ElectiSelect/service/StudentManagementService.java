package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.AdminStudentDTO;
import com.pragun.ElectiSelect.model.FailedRowDTO;
import com.pragun.ElectiSelect.model.PromotionResultDTO;
import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.StudentImportResultDTO;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.DeptElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.OpenElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudentManagementService {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;

    public StudentManagementService(UserRepository userRepository,
                                   AcademicStateRepository academicStateRepository,
                                   OpenElectiveSelectionRepository openElectiveSelectionRepository,
                                   DeptElectiveSelectionRepository deptElectiveSelectionRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminStudentDTO> getStudents(String search,
                                            String department,
                                            Integer semester,
                                            Boolean eligible) {
        return userRepository.findAdminStudents(search, department, semester, eligible);
    }

    @Transactional(readOnly = true)
    public List<AdminStudentDTO> getAllStudentsForExport() {
        return userRepository.findAdminStudents(null, null, null, null);
    }

    @Transactional
    public AdminStudentDTO toggleEligibility(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

    AcademicState state = academicStateRepository.findById(studentId)
        .orElseThrow(() -> new IllegalArgumentException("Academic state not found"));

    boolean nextEligible = !state.isEligible();
    state.setEligible(nextEligible);
    AcademicState persisted = academicStateRepository.saveAndFlush(state);

        boolean openSubmitted = openElectiveSelectionRepository.existsByStudent_Id(studentId);
        boolean deptSubmitted = deptElectiveSelectionRepository.existsByStudent_Id(studentId);

        return new AdminStudentDTO(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getUsn(),
                student.getDepartment(),
                persisted.getCurrentSemester(),
                persisted.isEligible(),
                student.getRole(),
                openSubmitted,
                deptSubmitted
        );
    }

    @Transactional
    public AdminStudentDTO promoteStudent(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only STUDENT accounts can be promoted");
        }

        AcademicState state = academicStateRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Academic state not found"));

        if (state.getCurrentSemester() >= 8) {
            throw new IllegalArgumentException("Student already at maximum semester");
        }

        state.setCurrentSemester(state.getCurrentSemester() + 1);
        AcademicState persisted = academicStateRepository.saveAndFlush(state);

        boolean openSubmitted = openElectiveSelectionRepository.existsByStudent_Id(studentId);
        boolean deptSubmitted = deptElectiveSelectionRepository.existsByStudent_Id(studentId);

        return new AdminStudentDTO(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getUsn(),
                student.getDepartment(),
                persisted.getCurrentSemester(),
                persisted.isEligible(),
                student.getRole(),
                openSubmitted,
                deptSubmitted
        );
    }

    @Transactional
    public PromotionResultDTO promoteBulk(int semester) {
        if (semester >= 8) {
            return new PromotionResultDTO(semester, 0, 0, "Student already at maximum semester");
        }
        if (semester < 1) {
            return new PromotionResultDTO(semester, 0, 0, "Invalid semester provided");
        }

        long total = academicStateRepository.countByCurrentSemester(semester);
        int promoted = academicStateRepository.bulkPromoteSemester(semester);
        int skipped = (int) Math.max(0, total - promoted);

        return new PromotionResultDTO(
                semester,
                promoted,
                skipped,
                promoted > 0 ? "Promotion completed" : "No students promoted"
        );
    }

    // ── Bulk Student CSV Import ────────────────────────────────────────────

    private static final Set<String> ALLOWED_DOMAINS = Set.of("@dsce.edu.in", "@dayanandasagar.edu");

    /**
     * Imports students from a CSV file.
     * CSV columns: name,email,usn,department,currentSemester,isEligible
     *
     * Rules (per row — NO global rollback):
     *  - email domain must be @dsce.edu.in or @dayanandasagar.edu
     *  - currentSemester must be 1-8
     *  - email must not already exist (database OR current batch)
     *  - usn must not already exist (database OR current batch)
     *  - isEligible defaults to true if blank/missing
     *
     * Valid rows are saved immediately; invalid rows are recorded in failedRows.
     */
    public StudentImportResultDTO importStudentsFromCsv(MultipartFile file) {
        List<FailedRowDTO> failedRows = new ArrayList<>();
        int importedCount = 0;
        int rowNumber = 0;

        // Track emails/USNs seen within this batch to catch intra-file duplicates
        Set<String> batchEmails = new HashSet<>();
        Set<String> batchUsns  = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Skip header row
                if (firstLine) {
                    firstLine = false;
                    if (line.toLowerCase().startsWith("name")) continue;
                }

                rowNumber++;
                String[] cols = line.split(",", -1);

                // Column count check
                if (cols.length < 5) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Insufficient columns (expected at least 5: name,email,usn,department,currentSemester)"));
                    continue;
                }

                String name       = cols[0].trim();
                String email      = cols[1].trim().toLowerCase();
                String usn        = cols[2].trim().toUpperCase();
                String department = cols[3].trim();
                String semStr     = cols[4].trim();
                String eligStr    = cols.length > 5 ? cols[5].trim() : "";

                // Name check
                if (name.isEmpty()) {
                    failedRows.add(new FailedRowDTO(rowNumber, "Name is required"));
                    continue;
                }

                // Email presence check
                if (email.isEmpty()) {
                    failedRows.add(new FailedRowDTO(rowNumber, "Email is required"));
                    continue;
                }

                // Email domain validation
                boolean domainAllowed = ALLOWED_DOMAINS.stream().anyMatch(email::endsWith);
                if (!domainAllowed) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Email domain not allowed: " + email));
                    continue;
                }

                // USN check
                if (usn.isEmpty()) {
                    failedRows.add(new FailedRowDTO(rowNumber, "USN is required"));
                    continue;
                }

                // Department check
                if (department.isEmpty()) {
                    failedRows.add(new FailedRowDTO(rowNumber, "Department is required"));
                    continue;
                }

                // Semester validation
                int semester;
                try {
                    semester = Integer.parseInt(semStr);
                } catch (NumberFormatException e) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Invalid semester value: '" + semStr + "'"));
                    continue;
                }
                if (semester < 1 || semester > 8) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Semester must be 1-8, got: " + semester));
                    continue;
                }

                // Eligibility (default true if blank)
                boolean isEligible = eligStr.isEmpty() || Boolean.parseBoolean(eligStr);

                // Intra-batch duplicate check
                if (!batchEmails.add(email)) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Duplicate email in this file: " + email));
                    continue;
                }
                if (!batchUsns.add(usn)) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Duplicate USN in this file: " + usn));
                    continue;
                }

                // Database duplicate check
                if (userRepository.existsByEmail(email)) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Email already exists: " + email));
                    continue;
                }
                if (userRepository.existsByUsn(usn)) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "USN already exists: " + usn));
                    continue;
                }

                // Persist valid row (own transaction so failure doesn't roll back prior rows)
                try {
                    saveStudentRow(name, email, usn, department, semester, isEligible);
                    importedCount++;
                } catch (Exception e) {
                    failedRows.add(new FailedRowDTO(rowNumber,
                            "Save failed: " + e.getMessage()));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }

        int totalRows = rowNumber;
        int skippedCount = failedRows.size();
        String message = importedCount > 0
                ? "Import complete: " + importedCount + " student(s) imported, " + skippedCount + " skipped."
                : "No students were imported. " + skippedCount + " row(s) had errors.";

        return new StudentImportResultDTO(totalRows, importedCount, skippedCount, failedRows, message);
    }

    /**
     * Saves a single student (User + AcademicState) in its own transaction
     * so a failure on one row does NOT roll back previously committed rows.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStudentRow(String name, String email, String usn,
                               String department, int semester, boolean isEligible) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setUsn(usn);
        user.setDepartment(department);
        user.setRole(Role.STUDENT);
        user.setProfileCompleted(true);
        User savedUser = userRepository.save(user);

        AcademicState state = new AcademicState();
        state.setUser(savedUser);
        state.setCurrentSemester(semester);
        state.setEligible(isEligible);
        academicStateRepository.save(state);
    }
}
