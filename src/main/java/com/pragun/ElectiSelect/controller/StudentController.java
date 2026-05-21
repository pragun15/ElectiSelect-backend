package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.ProfileResponse;
import com.pragun.ElectiSelect.model.SubjectDTO;
import com.pragun.ElectiSelect.model.User;
import com.pragun.ElectiSelect.repository.DeptElectiveSelectionRepository;
import com.pragun.ElectiSelect.repository.OpenElectiveSelectionRepository;
import com.pragun.ElectiSelect.service.RegistrationService;
import com.pragun.ElectiSelect.service.SubjectService;
import com.pragun.ElectiSelect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final RegistrationService registrationService;
    private final SubjectService subjectService;
    private final UserService userService;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;

    public StudentController(RegistrationService registrationService,
                             SubjectService subjectService,
                             UserService userService,
                             OpenElectiveSelectionRepository openElectiveSelectionRepository,
                             DeptElectiveSelectionRepository deptElectiveSelectionRepository) {
        this.registrationService = registrationService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getStudentProfile(email));
    }

    @GetMapping("/available-subjects")
    @PreAuthorize("hasAnyRole('STUDENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> getSubjects(@AuthenticationPrincipal String email) {
        try {
            User student = userService.getUserByEmail(email);
            int semester = userService.getStudentSemester(email);

            List<SubjectDTO> available = subjectService
                    .getAvailableOpenSubjectsForStudent(semester, student.getDepartment());

            System.out.println("🔍 fetchSubjects called for semester: " + semester + ", department: " + student.getDepartment());
            System.out.println("🔍 Found " + available.size() + " available subjects.");

            return ResponseEntity.ok(available);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }

    @PostMapping("/register/{subjectId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'SUPER_ADMIN')")
    public ResponseEntity<String> register(@AuthenticationPrincipal String email,
                                           @PathVariable Long subjectId) {
        registrationService.registerStudentForElective(email, subjectId);
        return ResponseEntity.ok("Registration successful!");
    }

    /**
     * GET /api/student/my-selection
     *
     * Returns the subject ID of the student's current open-elective selection,
     * or null if they have not selected yet.
     *
     * The frontend uses this on page load to pre-highlight the already-selected subject.
     * This endpoint is read-only — it does NOT modify any state.
     */
    @GetMapping("/my-selection")
    @PreAuthorize("hasAnyRole('STUDENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> getMySelection(@AuthenticationPrincipal String email) {
        try {
            User student = userService.getUserByEmail(email);
            return openElectiveSelectionRepository
                    .findFirstByStudentOrderByIdDesc(student)
                    .map(sel -> ResponseEntity.ok((Object) sel.getSubject().getId()))
                    .orElse(ResponseEntity.ok(null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching selection: " + e.getMessage());
        }
    }

    @GetMapping("/my-selections")
    @PreAuthorize("hasAnyRole('STUDENT', 'SUPER_ADMIN')")
    public ResponseEntity<?> getMySelections(@AuthenticationPrincipal String email) {
        try {
            User student = userService.getUserByEmail(email);
            com.pragun.ElectiSelect.model.MySelectionsResponse response = new com.pragun.ElectiSelect.model.MySelectionsResponse();

            openElectiveSelectionRepository.findFirstByStudentOrderByIdDesc(student).ifPresent(sel -> {
                com.pragun.ElectiSelect.model.Subject subject = sel.getSubject();
                boolean isActive = subject.getSession() != null && subject.getSession().getIsActive() != null && subject.getSession().getIsActive();
                response.setOpenElective(new com.pragun.ElectiSelect.model.SelectionDetailDTO(
                        true,
                        subject.getCourseCode(),
                        subject.getTitle(),
                        isActive
                ));
            });

            deptElectiveSelectionRepository.findFirstByStudentOrderByIdDesc(student).ifPresent(sel -> {
                com.pragun.ElectiSelect.model.Subject subject = sel.getSubject();
                boolean isActive = subject.getSession() != null && subject.getSession().getIsActive() != null && subject.getSession().getIsActive();
                response.setDepartmentElective(new com.pragun.ElectiSelect.model.SelectionDetailDTO(
                        true,
                        subject.getCourseCode(),
                        subject.getTitle(),
                        isActive
                ));
            });

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error fetching selections: " + e.getMessage());
        }
    }
}