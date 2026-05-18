package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.AcademicStateRepository;
import com.pragun.ElectiSelect.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;

    public UserController(UserRepository userRepository, AcademicStateRepository academicStateRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
    }

    /**
     * GET /api/user/me
     * Returns the current user's identity and profile completion status.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer semester = null;
        if (user.getRole() == Role.STUDENT) {
            semester = academicStateRepository.findByUser(user)
                    .map(AcademicState::getCurrentSemester)
                    .orElse(0);
        }

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getName() != null ? user.getName() : "",
                "role", user.getRole() != null ? user.getRole().name() : "",
                "department", user.getDepartment() != null ? user.getDepartment() : "",
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "profileCompleted", user.isProfileCompleted(),
                "semester", semester != null ? semester : 0
        ));
    }

    /**
     * POST /api/user/complete-profile
     * Completes the user's profile on first login. Blocked for SUPER_ADMIN.
     */
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@AuthenticationPrincipal String email,
                                              @RequestBody ProfileCompletionRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("error", "SUPER_ADMIN cannot use this endpoint"));
        }

        // Validate required fields
        if (req.getName() == null || req.getName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
        }
        if (req.getPhone() == null || req.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone is required"));
        }
        if (req.getDepartment() == null || req.getDepartment().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Department is required"));
        }
        if (user.getRole() == Role.STUDENT) {
            if (req.getSemester() == null || req.getSemester() < 1) {
                return ResponseEntity.badRequest().body(Map.of("error", "Semester is required for students"));
            }
            if (req.getUsn() == null || req.getUsn().trim().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "USN is required for students"));
            }

            // Identity Consistency Validation
            String emailPrefix = email.substring(0, email.indexOf('@')).trim().toUpperCase();
            String enteredUsn = req.getUsn().trim().toUpperCase();

            if (!emailPrefix.equals(enteredUsn)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and USN do not match."));
            }

            user.setUsn(enteredUsn);
        }

        // Update user fields
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setDepartment(req.getDepartment());
        user.setProfileCompleted(true);
        userRepository.save(user);

        // Update AcademicState semester for students
        if (user.getRole() == Role.STUDENT) {
            AcademicState state = academicStateRepository.findByUser(user)
                    .orElseGet(() -> {
                        AcademicState s = new AcademicState();
                        s.setUser(user);
                        return s;
                    });
            state.setCurrentSemester(req.getSemester());
            academicStateRepository.save(state);
        }

        return ResponseEntity.ok(Map.of("message", "Profile completed successfully"));
    }
}
