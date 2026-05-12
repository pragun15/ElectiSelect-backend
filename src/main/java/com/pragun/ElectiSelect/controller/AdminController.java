package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.AdminDashboardStatsDTO;
import com.pragun.ElectiSelect.model.AdminSessionDTO;
import com.pragun.ElectiSelect.model.AdminStudentDTO;
import com.pragun.ElectiSelect.model.AdminStudentRowDTO;
import com.pragun.ElectiSelect.model.PopularElectiveDTO;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    private final SessionService sessionService;
    private final SubjectService subjectService;
    private final RegistrationService registrationService;
    private final AdminDashboardService adminDashboardService;
    private final StudentManagementService studentManagementService;

    public AdminController(SessionService sessionService,
                           SubjectService subjectService,
                           RegistrationService registrationService,
                           AdminDashboardService adminDashboardService,
                           StudentManagementService studentManagementService) {
        this.sessionService = sessionService;
        this.subjectService = subjectService;
        this.registrationService = registrationService;
        this.adminDashboardService = adminDashboardService;
        this.studentManagementService = studentManagementService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody Session session) {
        return ResponseEntity.ok(sessionService.createSession(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionDTO>> getAllSessions() {
        return ResponseEntity.ok(adminDashboardService.getSessionOverview());
    }

    @PostMapping("/sessions/{sessionId}/upload-subjects")
    public ResponseEntity<String> uploadSubjects(@PathVariable Long sessionId, @RequestParam("file") MultipartFile file) {
        try {
            subjectService.uploadSubjectsFromExcel(file, sessionId);
            return ResponseEntity.ok("Subjects uploaded successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/sessions/export")
    public void exportData(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=registrations.xlsx");

        var registrations = registrationService.getAllRegistrations();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Registrations");
            // Excel generation logic stays here or moves to a dedicated ExcelUtility class
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Student Name");
            header.createCell(1).setCellValue("Subject");

            int rowIdx = 1;
            for (var reg : registrations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(reg.getUser().getName());
                row.createCell(1).setCellValue(reg.getSubject().getTitle());
            }
            workbook.write(response.getOutputStream());
        }
    }

    // ── Dashboard (read-only) ───────────────────────────────────────────────

    @GetMapping("/dashboard/stats")
    public ResponseEntity<AdminDashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDashboardStats());
    }

    @GetMapping("/dashboard/students")
    public ResponseEntity<List<AdminStudentRowDTO>> getStudentRows() {
        return ResponseEntity.ok(adminDashboardService.getStudentRows());
    }

    @GetMapping("/dashboard/sessions")
    public ResponseEntity<List<AdminSessionDTO>> getSessionOverview() {
        return ResponseEntity.ok(adminDashboardService.getSessionOverview());
    }

    @GetMapping("/dashboard/popular-electives")
    public ResponseEntity<List<PopularElectiveDTO>> getPopularElectives(
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminDashboardService.getPopularElectives(limit));
    }

    // ── Student Management (System Admin) ──────────────────────────────────

    @GetMapping("/students")
    public ResponseEntity<List<AdminStudentDTO>> getStudents(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "department", required = false) String department,
            @RequestParam(name = "semester", required = false) Integer semester,
            @RequestParam(name = "eligible", required = false) Boolean eligible) {
        return ResponseEntity.ok(studentManagementService.getStudents(search, department, semester, eligible));
    }

    @PatchMapping("/students/{id}/eligibility")
    public ResponseEntity<AdminStudentDTO> toggleEligibility(@PathVariable("id") Long id) {
        return ResponseEntity.ok(studentManagementService.toggleEligibility(id));
    }
}

