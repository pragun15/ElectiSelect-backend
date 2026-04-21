package com.pragun.ElectiSelect.controller;

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

    public AdminController(SessionService sessionService, SubjectService subjectService, RegistrationService registrationService) {
        this.sessionService = sessionService;
        this.subjectService = subjectService;
        this.registrationService = registrationService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody Session session) {
        return ResponseEntity.ok(sessionService.createSession(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getAllSessions() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    @PostMapping("/sessions/{sessionId}/upload-subjects")
    public ResponseEntity<String> uploadSubjects(@PathVariable Long sessionId, @RequestParam("file") MultipartFile file) {
        try {
            subjectService.uploadSubjectsFromExcel(file, sessionId);
            return ResponseEntity.ok("Subjects uploaded successfully.");
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
}

