package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.DeptCategoryDTO;
import com.pragun.ElectiSelect.model.DeptElectiveSelectionRequest;
import com.pragun.ElectiSelect.model.DeptElectiveSelectionStatusResponse;
import com.pragun.ElectiSelect.model.ElectiveStatusResponse;
import com.pragun.ElectiSelect.service.DeptElectiveSelectionService;
import com.pragun.ElectiSelect.service.ElectiveStatusService;
import com.pragun.ElectiSelect.service.SubjectService;
import com.pragun.ElectiSelect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/electives")
public class ElectivesController {

    private final SubjectService subjectService;
    private final UserService userService;
    private final DeptElectiveSelectionService deptElectiveSelectionService;
    private final ElectiveStatusService electiveStatusService;

    public ElectivesController(SubjectService subjectService,
                               UserService userService,
                               DeptElectiveSelectionService deptElectiveSelectionService,
                               ElectiveStatusService electiveStatusService) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.deptElectiveSelectionService = deptElectiveSelectionService;
        this.electiveStatusService = electiveStatusService;
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

    /**
     * POST /api/electives/dept/select
     *
     * Accepts a list of { categoryId, subjectId } selections.
     * Enforces validation and persists all selections atomically.
     */
    @PostMapping("/dept/select")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitDeptElectives(@AuthenticationPrincipal String email,
                                                 @RequestBody List<DeptElectiveSelectionRequest> requests) {
        deptElectiveSelectionService.submitSelections(email, requests == null ? Collections.emptyList() : requests);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/electives/open/status
     */
    @GetMapping("/open/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ElectiveStatusResponse> getOpenStatus(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(electiveStatusService.getOpenStatus(email));
    }

    /**
     * GET /api/electives/dept/status
     */
    @GetMapping("/dept/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ElectiveStatusResponse> getDeptStatus(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(electiveStatusService.getDeptStatus(email));
    }

    /**
     * GET /api/electives/dept/my-selection
     *
     * Returns whether the student has submitted department electives
     * for the active DEPARTMENT session and includes selection summaries.
     */
    @GetMapping("/dept/my-selection")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DeptElectiveSelectionStatusResponse> getMyDeptSelections(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(deptElectiveSelectionService.getMySelections(email));
    }
}
