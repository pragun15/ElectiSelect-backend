package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.*;
import com.pragun.ElectiSelect.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyRole('STAFF','ISE_ADMIN','SUPER_ADMIN')")
public class StaffController {

    private final AdminDashboardService adminDashboardService;
    private final UserRepository userRepository;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;
    private final SessionRepository sessionRepository;

    public StaffController(AdminDashboardService adminDashboardService,
                           UserRepository userRepository,
                           OpenElectiveSelectionRepository openElectiveSelectionRepository,
                           DeptElectiveSelectionRepository deptElectiveSelectionRepository,
                           SessionRepository sessionRepository) {
        this.adminDashboardService = adminDashboardService;
        this.userRepository = userRepository;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
        this.sessionRepository = sessionRepository;
    }

    private boolean isIseAuthorized(String email) {
        return userRepository.findByEmail(email)
                .map(u -> "ISE".equalsIgnoreCase(u.getDepartment())
                        || u.getRole() == Role.SUPER_ADMIN
                        || u.getRole() == Role.ISE_ADMIN)
                .orElse(false);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<AdminSessionDTO>> getAllSessions() {
        return ResponseEntity.ok(adminDashboardService.getSessionOverview());
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(
            @AuthenticationPrincipal String email,
            @RequestParam(name = "limit", defaultValue = "5") int limit,
            AnalyticsFilterDTO filter) {

        boolean deptRequested = filter != null
                && "DEPARTMENT".equalsIgnoreCase(filter.getType());

        if (deptRequested && !isIseAuthorized(email)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Department elective data is restricted to ISE staff."));
        }

        return ResponseEntity.ok(adminDashboardService.getAdminAnalytics(limit, filter));
    }

    @GetMapping("/export/registrations/open")
    public void exportOpenRegistrations(
            @AuthenticationPrincipal String email,
            @RequestParam Long sessionId,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=open-registrations-session-" + sessionId + ".xlsx");

        List<OpenElectiveSelectionRepository.StaffExportProjection> selections = openElectiveSelectionRepository.findExportDataBySessionId(sessionId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Open Registrations");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Student Name");
            header.createCell(1).setCellValue("USN");
            header.createCell(2).setCellValue("Department");
            header.createCell(3).setCellValue("Subject Title");
            header.createCell(4).setCellValue("Course Code");

            int rowIdx = 1;
            for (OpenElectiveSelectionRepository.StaffExportProjection sel : selections) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sel.getStudentName());
                row.createCell(1).setCellValue(sel.getUsn() != null ? sel.getUsn() : "");
                row.createCell(2).setCellValue(sel.getDepartment());
                row.createCell(3).setCellValue(sel.getSubjectTitle());
                row.createCell(4).setCellValue(sel.getCourseCode());
            }
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export/registrations/dept")
    public void exportDeptRegistrations(
            @AuthenticationPrincipal String email,
            @RequestParam Long sessionId,
            HttpServletResponse response) throws IOException {

        if (!isIseAuthorized(email)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Department elective exports are restricted to ISE staff.");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=dept-registrations-session-" + sessionId + ".xlsx");

        List<DeptElectiveSelectionRepository.StaffExportProjection> selections = deptElectiveSelectionRepository.findExportDataBySessionId(sessionId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Dept Registrations");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Student Name");
            header.createCell(1).setCellValue("USN");
            header.createCell(2).setCellValue("Department");
            header.createCell(3).setCellValue("Category");
            header.createCell(4).setCellValue("Subject Title");
            header.createCell(5).setCellValue("Course Code");

            int rowIdx = 1;
            for (DeptElectiveSelectionRepository.StaffExportProjection sel : selections) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sel.getStudentName());
                row.createCell(1).setCellValue(sel.getUsn() != null ? sel.getUsn() : "");
                row.createCell(2).setCellValue(sel.getDepartment());
                row.createCell(3).setCellValue(sel.getCategoryName() != null ? sel.getCategoryName() : "");
                row.createCell(4).setCellValue(sel.getSubjectTitle());
                row.createCell(5).setCellValue(sel.getCourseCode());
            }
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export/registrations/open/csv")
    public void exportOpenRegistrationsCsv(
            @AuthenticationPrincipal String email,
            @RequestParam Long sessionId,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=open-registrations-session-" + sessionId + ".csv");

        List<OpenElectiveSelectionRepository.StaffExportProjection> selections = openElectiveSelectionRepository.findExportDataBySessionId(sessionId);

        StringBuilder csv = new StringBuilder();
        csv.append("Student Name,USN,Department,Subject Title,Course Code\n");

        for (OpenElectiveSelectionRepository.StaffExportProjection sel : selections) {
            csv.append(toCsvField(sel.getStudentName())).append(',')
               .append(toCsvField(sel.getUsn())).append(',')
               .append(toCsvField(sel.getDepartment())).append(',')
               .append(toCsvField(sel.getSubjectTitle())).append(',')
               .append(toCsvField(sel.getCourseCode())).append('\n');
        }

        response.getWriter().write(csv.toString());
    }

    @GetMapping("/export/registrations/dept/csv")
    public void exportDeptRegistrationsCsv(
            @AuthenticationPrincipal String email,
            @RequestParam Long sessionId,
            HttpServletResponse response) throws IOException {

        if (!isIseAuthorized(email)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Department elective exports are restricted to ISE staff.");
            return;
        }

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=dept-registrations-session-" + sessionId + ".csv");

        List<DeptElectiveSelectionRepository.StaffExportProjection> selections = deptElectiveSelectionRepository.findExportDataBySessionId(sessionId);

        StringBuilder csv = new StringBuilder();
        csv.append("Student Name,USN,Department,Category,Subject Title,Course Code\n");

        for (DeptElectiveSelectionRepository.StaffExportProjection sel : selections) {
            csv.append(toCsvField(sel.getStudentName())).append(',')
               .append(toCsvField(sel.getUsn())).append(',')
               .append(toCsvField(sel.getDepartment())).append(',')
               .append(toCsvField(sel.getCategoryName())).append(',')
               .append(toCsvField(sel.getSubjectTitle())).append(',')
               .append(toCsvField(sel.getCourseCode())).append('\n');
        }

        response.getWriter().write(csv.toString());
    }

    private String toCsvField(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ");
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }
}
