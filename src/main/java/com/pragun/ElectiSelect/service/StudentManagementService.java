package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.AcademicState;
import com.pragun.ElectiSelect.model.AdminStudentDTO;
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
}
