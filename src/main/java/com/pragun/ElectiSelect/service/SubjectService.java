package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.Subject;
import com.pragun.ElectiSelect.repository.SessionRepository;
import com.pragun.ElectiSelect.repository.SubjectRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final SessionRepository sessionRepository;

    public SubjectService(SubjectRepository subjectRepository, SessionRepository sessionRepository) {
        this.subjectRepository = subjectRepository;
        this.sessionRepository = sessionRepository;
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

                if (row.getCell(4) != null) {
                    subject.setRestrictedDepts(row.getCell(4).getStringCellValue());
                }
                subjects.add(subject);
            }
            subjectRepository.saveAll(subjects);
        }
    }


    public List<Subject> getAvailableSubjectsForSemester(int semester) {
        return subjectRepository.findBySession_IsActiveTrueAndSession_Semester(semester);
    }
}