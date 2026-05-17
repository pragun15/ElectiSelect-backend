package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.DeptCategory;
import com.pragun.ElectiSelect.model.DeptCategoryDTO;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.model.Subject;
import com.pragun.ElectiSelect.model.SubjectDTO;
import com.pragun.ElectiSelect.model.SubjectUploadConfirmRequestDTO;
import com.pragun.ElectiSelect.model.SubjectUploadConfirmResultDTO;
import com.pragun.ElectiSelect.model.SubjectUploadErrorDTO;
import com.pragun.ElectiSelect.model.SubjectUploadPreviewDTO;
import com.pragun.ElectiSelect.model.SubjectUploadRowDTO;
import com.pragun.ElectiSelect.repository.DeptCategoryRepository;
import com.pragun.ElectiSelect.repository.SessionRepository;
import com.pragun.ElectiSelect.repository.SubjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository;
    private final DeptCategoryRepository deptCategoryRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          SessionRepository sessionRepository,
                          DeptCategoryRepository deptCategoryRepository) {
        this.subjectRepository = subjectRepository;
        this.sessionRepository = sessionRepository;
        this.deptCategoryRepository = deptCategoryRepository;
    }

    @Transactional
    public void uploadSubjectsFromExcel(MultipartFile file, Long sessionId) throws Exception {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getIsActive() != null && session.getIsActive()) {
            throw new RuntimeException("Cannot upload subjects into an active session.");
        }

        boolean hasSubjects = subjectRepository.existsNonDeletedBySessionId(sessionId);
        if (hasSubjects) {
            throw new RuntimeException("This session already contains uploaded subjects. Re-upload is currently disabled.");
        }

        java.util.Map<String, DeptCategory> categoryCache = new java.util.HashMap<>();
        List<Subject> subjects = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null) continue;

                Subject subject = new Subject();
                subject.setSession(session);
                
                if (row.getCell(0).getCellType() == CellType.STRING) {
                    subject.setCourseCode(row.getCell(0).getStringCellValue());
                } else if (row.getCell(0).getCellType() == CellType.NUMERIC) {
                    subject.setCourseCode(String.valueOf((int) row.getCell(0).getNumericCellValue()));
                } else {
                     throw new RuntimeException("Course Code must be provided for row " + (i + 1));
                }

                subject.setTitle(row.getCell(1).getStringCellValue());
                subject.setDepartment(row.getCell(2).getStringCellValue());
                subject.setMaxSeats((int) row.getCell(3).getNumericCellValue());
                subject.setFilled_seats(0);

                // Column 4: restricted_depts (optional blocklist)
                if (row.getCell(4) != null && !row.getCell(4).getStringCellValue().isBlank()) {
                    subject.setRestrictedDepts(row.getCell(4).getStringCellValue());
                }

                // Column 5: credits (MANDATORY — no default, no fallback)
                if (row.getCell(5) == null) {
                    throw new RuntimeException("Credits must be provided for row " + (i + 1) + " (course: " + subject.getCourseCode() + ")");
                }
                int credits = (int) row.getCell(5).getNumericCellValue();
                if (credits <= 0) {
                    throw new RuntimeException("Credits must be provided for row " + (i + 1) + " (course: " + subject.getCourseCode() + ")");
                }
                subject.setCredits(credits);

                // Column 6: Category Name
                String categoryName = null;
                if (row.getCell(6) != null && row.getCell(6).getCellType() == CellType.STRING && !row.getCell(6).getStringCellValue().isBlank()) {
                    categoryName = row.getCell(6).getStringCellValue().trim();
                }

                if (session.getType() == SessionType.DEPARTMENT && categoryName == null) {
                    throw new RuntimeException("Category Name must be provided for row " + (i + 1) + " (course: " + subject.getCourseCode() + ") because this is a DEPARTMENT session.");
                }

                if (categoryName != null) {
                    DeptCategory category = categoryCache.get(categoryName);
                    if (category == null) {
                        category = deptCategoryRepository.findByCategoryNameAndSession_Id(categoryName, sessionId);
                        if (category == null) {
                            category = new DeptCategory();
                            category.setCategoryName(categoryName);
                            category.setSession(session);
                            category = deptCategoryRepository.save(category);
                        }
                        categoryCache.put(categoryName, category);
                    }
                    subject.setCategory(category);
                }

                subjects.add(subject);
            }
            subjectRepository.saveAll(subjects);
        }
    }

    public SubjectUploadPreviewDTO previewSubjectUpload(MultipartFile file, Long sessionId) throws Exception {
        Session session = loadSessionForUpload(sessionId);
        ensureSessionUploadAllowed(sessionId, session);

        List<String> existingCodes = subjectRepository.findCourseCodesBySessionId(sessionId);
        Set<String> existingCodeSet = existingCodes.stream()
                .filter(code -> code != null)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        ParsedUploadResult parsed = parseSubjectWorkbook(file, existingCodeSet);
        return new SubjectUploadPreviewDTO(
                parsed.totalRows,
                parsed.validRows.size(),
                parsed.invalidRows.size(),
                parsed.validRows,
                parsed.invalidRows
        );
    }

    @Transactional
    public SubjectUploadConfirmResultDTO confirmSubjectUpload(SubjectUploadConfirmRequestDTO request) {
        if (request == null || request.getSessionId() == null) {
            throw new RuntimeException("Session must be selected before confirming upload.");
        }

        Session session = loadSessionForUpload(request.getSessionId());
        ensureSessionUploadAllowed(request.getSessionId(), session);

        List<String> existingCodes = subjectRepository.findCourseCodesBySessionId(request.getSessionId());
        Set<String> existingCodeSet = existingCodes.stream()
                .filter(code -> code != null)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> seenCodes = new HashSet<>();
        List<String> skippedCodes = new ArrayList<>();
        List<Subject> subjects = new ArrayList<>();

        if (request.getSubjects() != null) {
            for (SubjectUploadRowDTO row : request.getSubjects()) {
                String normalizedCode = normalizeCode(row == null ? null : row.getCourseCode());
                if (normalizedCode == null) {
                    skippedCodes.add(null);
                    continue;
                }
                if (existingCodeSet.contains(normalizedCode) || seenCodes.contains(normalizedCode)) {
                    skippedCodes.add(normalizedCode);
                    continue;
                }
                seenCodes.add(normalizedCode);

                Subject subject = new Subject();
                subject.setSession(session);
                subject.setCourseCode(normalizedCode);
                subject.setTitle(safeValue(row.getTitle()));
                subject.setDepartment(safeValue(row.getDepartment()));
                subject.setMaxSeats(row.getMaxSeats() == null ? 0 : row.getMaxSeats());
                subject.setFilled_seats(0);
                subject.setRestrictedDepts(normalizeRestricted(row.getRestrictedDepts()));
                subject.setCredits(row.getCredits() == null ? 0 : row.getCredits());
                subjects.add(subject);
            }
        }

        subjectRepository.saveAll(subjects);
        return new SubjectUploadConfirmResultDTO(subjects.size(), skippedCodes.size(), skippedCodes);
    }

    // Legacy — kept to avoid breaking other callers.
    public List<Subject> getAvailableSubjectsForSemester(int semester) {
        return subjectRepository.findBySession_IsActiveTrueAndSession_SemesterAndIsDeletedFalse(semester);
    }

    /**
     * Fetch open elective subjects visible to a specific student.
     *
     * Enforces (workflow.md §9):
     * 1. Session type == OPEN, is_active == true, semester == student.currentSemester
     * 2. Department access rule: restricted_depts (blocklist) — a subject with no restricted_depts is open to all
     * 3. Returns SubjectDTO with remainingSeats computed at list time
     *
     * Note: seat counts are point-in-time snapshots; the backend transaction is authoritative.
     */
    public List<SubjectDTO> getAvailableOpenSubjectsForStudent(int semester, String studentDepartment) {
        // Step 1: fetch subjects in an active OPEN session for this semester
        List<Subject> subjects = subjectRepository
                .findBySession_IsActiveTrueAndSession_SemesterAndSession_TypeAndIsDeletedFalse(semester, SessionType.OPEN);

        System.out.println("🔍 [SubjectService] Raw subjects from DB for semester=" + semester + ": " + subjects.size());
        for (Subject s : subjects) {
            System.out.println("  Subject: id=" + s.getId() + ", title=" + s.getTitle() + ", dept=" + s.getDepartment() + ", restrictedDepts=" + s.getRestrictedDepts() + ", isDeleted=" + s.getIsDeleted());
            if (s.getSession() != null) {
                System.out.println("    -> Session: id=" + s.getSession().getId() + ", type=" + s.getSession().getType() + ", semester=" + s.getSession().getSemester() + ", isActive=" + s.getSession().getIsActive());
            }
        }

        // Step 2: apply department access rule and map to DTO
        List<SubjectDTO> result = subjects.stream()
                .filter(subject -> isDepartmentPermitted(subject, studentDepartment))
                .map(SubjectDTO::new)
                .collect(Collectors.toList());

        System.out.println("🔍 [SubjectService] After dept filter for '" + studentDepartment + "': " + result.size() + " subjects.");
        return result;
    }

    /**
     * Returns true if the student's department is permitted to select the given subject.
     * A subject with no restricted_depts is visible to ALL departments.
     * A subject with restricted_depts excludes the listed departments (comma-separated blocklist).
     */
    private boolean isDepartmentPermitted(Subject subject, String studentDepartment) {
        if (studentDepartment == null) return false;

        String restricted = subject.getRestrictedDepts();
        if (restricted == null || restricted.isBlank()) {
            return true; // No restriction — visible to everyone
        }

        return Arrays.stream(restricted.split(","))
                .map(String::trim)
                .noneMatch(dept -> dept.equalsIgnoreCase(studentDepartment));
    }

    private Session loadSessionForUpload(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    private void ensureSessionUploadAllowed(Long sessionId, Session session) {
        if (session.getIsActive() != null && session.getIsActive()) {
            throw new RuntimeException("Cannot upload subjects into an active session.");
        }

        boolean hasSubjects = subjectRepository.existsNonDeletedBySessionId(sessionId);
        if (hasSubjects) {
            throw new RuntimeException("This session already contains uploaded subjects. Re-upload is currently disabled.");
        }
    }

    private ParsedUploadResult parseSubjectWorkbook(MultipartFile file, Set<String> existingCodes) throws Exception {
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            HeaderMapping header = findHeaderRow(sheet, formatter);
            if (header == null) {
                throw new RuntimeException("Unable to locate header row. Please ensure the file contains Course Code and Course Title columns.");
            }

            List<SubjectUploadRowDTO> validRows = new ArrayList<>();
            List<SubjectUploadErrorDTO> invalidRows = new ArrayList<>();
            Set<String> seenCodes = new HashSet<>();
            int totalRows = 0;

            for (int i = header.rowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }

                totalRows++;
                String courseCode = getCellValue(row, header.courseCodeIndex, formatter);
                String title = getCellValue(row, header.titleIndex, formatter);
                String department = getCellValue(row, header.departmentIndex, formatter);
                String maxSeatsRaw = getCellValue(row, header.maxSeatsIndex, formatter);
                String restrictedRaw = header.restrictedIndex == null ? null : getCellValue(row, header.restrictedIndex, formatter);
                String creditsRaw = header.creditsIndex == null ? null : getCellValue(row, header.creditsIndex, formatter);

                String normalizedCode = normalizeCode(courseCode);
                String normalizedTitle = safeValue(title);
                String normalizedDepartment = safeValue(department);

                String error = null;
                if (normalizedCode == null) {
                    error = "Missing course code";
                } else if (normalizedTitle == null) {
                    error = "Missing course title";
                } else if (normalizedDepartment == null) {
                    error = "Missing department";
                }

                Integer maxSeats = parseInteger(maxSeatsRaw);
                if (error == null && (maxSeats == null || maxSeats <= 0)) {
                    error = "Invalid seat count";
                }

                Integer credits = parseInteger(creditsRaw);
                if (credits == null) {
                    credits = 0;
                }

                if (error == null) {
                    if (existingCodes.contains(normalizedCode)) {
                        error = "Duplicate course code in database";
                    } else if (seenCodes.contains(normalizedCode)) {
                        error = "Duplicate course code in file";
                    }
                }

                if (error != null) {
                    invalidRows.add(new SubjectUploadErrorDTO(i + 1, normalizedCode, normalizedTitle, error));
                    continue;
                }

                seenCodes.add(normalizedCode);
                validRows.add(new SubjectUploadRowDTO(
                        normalizedCode,
                        normalizedTitle,
                        normalizedDepartment,
                        maxSeats,
                        normalizeRestricted(restrictedRaw),
                        credits
                ));
            }

            return new ParsedUploadResult(totalRows, validRows, invalidRows);
        }
    }

    private HeaderMapping findHeaderRow(Sheet sheet, DataFormatter formatter) {
        int maxScanRows = Math.min(sheet.getLastRowNum(), 50);
        for (int i = 0; i <= maxScanRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : row) {
                String value = formatter.formatCellValue(cell);
                if (value == null) {
                    continue;
                }
                // Normalize: trim outer whitespace and collapse internal repeated spaces
                String normalized = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
                if (normalized.isEmpty()) {
                    continue;
                }

                // Course Code aliases: "course code", "subject code", "code"
                if (normalized.contains("course code")
                        || normalized.contains("subject code")
                        || normalized.equals("code")) {
                    headerMap.put("courseCode", cell.getColumnIndex());

                // Course Title aliases: "course title", "subject title", "title"
                } else if (normalized.contains("course title")
                        || normalized.contains("subject title")
                        || normalized.equals("title")) {
                    headerMap.put("title", cell.getColumnIndex());

                // Department aliases: "department name", "department", "dept"
                } else if (normalized.contains("department")
                        || normalized.equals("dept")) {
                    headerMap.put("department", cell.getColumnIndex());

                // Max Seats aliases: "max no. of students", "max seats", "seats"
                } else if ((normalized.contains("max") && normalized.contains("student"))
                        || normalized.equals("max seats")
                        || normalized.equals("seats")) {
                    headerMap.put("maxSeats", cell.getColumnIndex());

                // Restricted Departments aliases: "should not be offered to",
                // "not be offered", "restricted departments", "restricted depts"
                } else if (normalized.contains("should not")
                        || normalized.contains("not be offered")
                        || normalized.contains("restricted department")
                        || normalized.contains("restricted dept")) {
                    headerMap.put("restricted", cell.getColumnIndex());

                // Credits aliases: "credits", "credit"
                } else if (normalized.contains("credit")) {
                    headerMap.put("credits", cell.getColumnIndex());
                }
            }

            if (headerMap.containsKey("courseCode") && headerMap.containsKey("title") && headerMap.containsKey("maxSeats")) {
                HeaderMapping mapping = new HeaderMapping();
                mapping.rowIndex = i;
                mapping.courseCodeIndex = headerMap.get("courseCode");
                mapping.titleIndex = headerMap.get("title");
                mapping.departmentIndex = headerMap.get("department");
                mapping.maxSeatsIndex = headerMap.get("maxSeats");
                mapping.restrictedIndex = headerMap.get("restricted");
                mapping.creditsIndex = headerMap.get("credits");
                return mapping;
            }
        }
        return null;
    }

    private String getCellValue(Row row, Integer index, DataFormatter formatter) {
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value == null ? null : value.trim();
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (Cell cell : row) {
            String value = formatter.formatCellValue(cell);
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String safeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRestricted(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (lowered.equals("-") || lowered.equals("--") || lowered.equals("---") || lowered.equals("----") || lowered.equals("na") || lowered.equals("n/a")) {
            return null;
        }

        List<String> parts = Arrays.stream(normalized.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(",", parts);
    }

    private static class HeaderMapping {
        private int rowIndex;
        private Integer courseCodeIndex;
        private Integer titleIndex;
        private Integer departmentIndex;
        private Integer maxSeatsIndex;
        private Integer restrictedIndex;
        private Integer creditsIndex;
    }

    private static class ParsedUploadResult {
        private final int totalRows;
        private final List<SubjectUploadRowDTO> validRows;
        private final List<SubjectUploadErrorDTO> invalidRows;

        private ParsedUploadResult(int totalRows, List<SubjectUploadRowDTO> validRows, List<SubjectUploadErrorDTO> invalidRows) {
            this.totalRows = totalRows;
            this.validRows = validRows;
            this.invalidRows = invalidRows;
        }
    }

    /**
     * Fetch department electives grouped by category, for the given student's semester.
     *
     * Enforces:
     * 1. Session type == DEPARTMENT, is_active == true, semester == student.currentSemester
     * 2. Categories are fetched by active DEPARTMENT session id
     * 3. Subjects per category are fetched using category.id + session.id, excluding deleted
     * 4. Returns List<DeptCategoryDTO> with subjects nested under each category
     *
     * Open-elective logic is NOT touched.
     */
    public List<DeptCategoryDTO> getDeptElectivesForStudent(int semester) {
        // Step 1: resolve active DEPARTMENT session for this semester
        List<Session> deptSessions = sessionRepository
                .findByIsActiveTrueAndSemesterAndType(semester, SessionType.DEPARTMENT);

        System.out.println("🏫 [DeptElective] Looking for active DEPARTMENT session for semester=" + semester);
        System.out.println("🏫 [DeptElective] Found " + deptSessions.size() + " DEPARTMENT session(s).");

        if (deptSessions.isEmpty()) {
            System.out.println("🏫 [DeptElective] No active DEPARTMENT session — returning empty list.");
            return new ArrayList<>();
        }

        Session deptSession = deptSessions.get(0);
        System.out.println("🏫 [DeptElective] Using DEPARTMENT session id=" + deptSession.getId()
                + ", semester=" + deptSession.getSemester()
                + ", isActive=" + deptSession.getIsActive());

        // Step 2: fetch all categories linked to this session
        List<DeptCategory> categories = deptCategoryRepository.findBySession_Id(deptSession.getId());
        System.out.println("🏫 [DeptElective] Categories found: " + categories.size());

        if (categories.isEmpty()) {
            System.out.println("🏫 [DeptElective] ⚠️ No categories linked to session id=" + deptSession.getId()
                    + ". Check that dept_category.session_id matches the active DEPARTMENT session.");
            return new ArrayList<>();
        }

        // Step 3: for each category, fetch subjects and map to DTO
        List<DeptCategoryDTO> result = new ArrayList<>();
        for (DeptCategory category : categories) {
            List<Subject> rawSubjects = subjectRepository
                    .findByCategoryIdAndSessionIdAndNotDeleted(category.getId(), deptSession.getId());

            System.out.println("🏫 [DeptElective] Category '" + category.getCategoryName()
                    + "' (id=" + category.getId() + ") → " + rawSubjects.size() + " subject(s).");

            List<SubjectDTO> subjectDTOs = rawSubjects.stream()
                    .map(SubjectDTO::new)
                    .collect(Collectors.toList());

            result.add(new DeptCategoryDTO(category, subjectDTOs));
        }

        System.out.println("🏫 [DeptElective] Returning " + result.size() + " categories.");
        return result;
    }
}