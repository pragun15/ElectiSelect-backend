package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.ProfileResponse;
import com.pragun.ElectiSelect.model.SubjectDTO;
import com.pragun.ElectiSelect.model.User;
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
    private final UserService userService;

    public StudentController(RegistrationService registrationService,
                             SubjectService subjectService,
                             UserService userService) {
        this.registrationService = registrationService;
        this.subjectService = subjectService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getStudentProfile(email));
    }

    @GetMapping("/available-subjects")
    public ResponseEntity<List<SubjectDTO>> getSubjects(@AuthenticationPrincipal String email) {
        User student = userService.getUserByEmail(email);
        int semester = userService.getStudentSemester(email);
        List<SubjectDTO> available = subjectService.getAvailableOpenSubjectsForStudent(
                semester, student.getDepartment());
        return ResponseEntity.ok(available);
    }

    @PostMapping("/register/{subjectId}")
    public ResponseEntity<String> register(@AuthenticationPrincipal String email,
                                           @PathVariable Long subjectId) {
        registrationService.registerStudentForElective(email, subjectId);
        return ResponseEntity.ok("Registration successful!");
    }
}