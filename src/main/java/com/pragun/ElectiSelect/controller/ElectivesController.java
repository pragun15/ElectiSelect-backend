package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.DeptCategoryDTO;
import com.pragun.ElectiSelect.service.SubjectService;
import com.pragun.ElectiSelect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/electives")
public class ElectivesController {

    private final SubjectService subjectService;
    private final UserService userService;

    public ElectivesController(SubjectService subjectService, UserService userService) {
        this.subjectService = subjectService;
        this.userService = userService;
    }

    /**
     * GET /api/electives/dept
     *
     * Returns department electives grouped by category for the authenticated student.
     * Semester is resolved from the student's AcademicState — NOT from a query param.
     * Session type is strictly DEPARTMENT — open-elective logic is not touched.
     *
     * Response shape:
     *   [ { categoryId, categoryName, subjects: [ { id, courseCode, title, ... } ] } ]
     */
    @GetMapping("/dept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getDeptElectives(@AuthenticationPrincipal String email) {
        try {
            int semester = userService.getStudentSemester(email);
            System.out.println("🏫 [ElectivesController] getDeptElectives called for email=" + email + ", semester=" + semester);

            List<DeptCategoryDTO> result = subjectService.getDeptElectivesForStudent(semester);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("Error fetching dept electives: " + e.getMessage()
                            + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }
}
