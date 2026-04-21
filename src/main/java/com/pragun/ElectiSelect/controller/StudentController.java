package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.Subject;
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
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final RegistrationService registrationService;
    private final SubjectService subjectService;
    private final UserService userService; // 👈 Replaced Repositories with UserService

    public StudentController(RegistrationService registrationService,
                             SubjectService subjectService,
                             UserService userService) {
        this.registrationService = registrationService;
        this.subjectService = subjectService;
        this.userService = userService;
    }

    @GetMapping("/available-subjects")
    public ResponseEntity<List<Subject>> getSubjects(@AuthenticationPrincipal String email) {
        // Business logic is now hidden inside services
        int currentSemester = userService.getStudentSemester(email);
        List<Subject> available = subjectService.getAvailableSubjectsForSemester(currentSemester);

        return ResponseEntity.ok(available);
    }

    @PostMapping("/register/{subjectId}")
    public ResponseEntity<String> register(@AuthenticationPrincipal String email, @PathVariable Long subjectId) {
        try {
            registrationService.registerStudentForElective(email, subjectId);
            return ResponseEntity.ok("Registration successful!");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}