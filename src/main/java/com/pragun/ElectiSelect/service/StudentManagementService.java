package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.AdminStudentDTO;
import com.pragun.ElectiSelect.model.PromotionResultDTO;
import com.pragun.ElectiSelect.model.Role;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.DeptElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.OpenElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
