package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.DeptCategory;
import com.pragun.ElectiSelect.model.DeptCategoryDTO;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.SessionType;
import com.pragun.ElectiSelect.model.Subject;
import com.pragun.ElectiSelect.model.SubjectDTO;
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
import java.util.List;
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

        List<Subject> subjects = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row.getCell(0) == null) continue;

                Subject subject = new Subject();
                subject.setSession(session);
                subject.setCourseCode(row.getCell(0).getStringCellValue());
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
                    throw new RuntimeException("Credits must be provided for row " + (i + 1) + " (course: " + row.getCell(0).getStringCellValue() + ")");
                }
                int credits = (int) row.getCell(5).getNumericCellValue();
                if (credits <= 0) {
                    throw new RuntimeException("Credits must be provided for row " + (i + 1) + " (course: " + row.getCell(0).getStringCellValue() + ")");
                }
                subject.setCredits(credits);

                subjects.add(subject);
            }
            subjectRepository.saveAll(subjects);
        }
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