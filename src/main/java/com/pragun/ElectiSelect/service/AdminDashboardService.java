package com.pragun.ElectiSelect.service;

import com.pragun.ElectiSelect.model.*;
import com.pragun.ElectiSelect.repository.*;
import com.pragun.ElectiSelect.repository.SelectionCountProjection;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {


    private final UserRepository userRepository;
    private final AcademicStateRepository academicStateRepository;
    private final SessionRepository sessionRepository;
    private final DeptCategoryRepository deptCategoryRepository;
    private final SubjectRepository subjectRepository;
    private final OpenElectiveSelectionRepository openElectiveSelectionRepository;
    private final DeptElectiveSelectionRepository deptElectiveSelectionRepository;

    public AdminDashboardService(UserRepository userRepository,
                                 AcademicStateRepository academicStateRepository,
                                 SessionRepository sessionRepository,
                                 DeptCategoryRepository deptCategoryRepository,
                                 SubjectRepository subjectRepository,
                                 OpenElectiveSelectionRepository openElectiveSelectionRepository,
                                 DeptElectiveSelectionRepository deptElectiveSelectionRepository) {
        this.userRepository = userRepository;
        this.academicStateRepository = academicStateRepository;
        this.sessionRepository = sessionRepository;
        this.deptCategoryRepository = deptCategoryRepository;
        this.subjectRepository = subjectRepository;
        this.openElectiveSelectionRepository = openElectiveSelectionRepository;
        this.deptElectiveSelectionRepository = deptElectiveSelectionRepository;
    }

    public AdminDashboardStatsDTO getDashboardStats() {
        List<AdminStudentRowDTO> rows = getStudentRows();

        long registeredStudents = rows.size();
        long openElectiveTaken = rows.stream().filter(AdminStudentRowDTO::isOpenElectiveSelected).count();
        long deptElectiveTaken = rows.stream().filter(AdminStudentRowDTO::isDeptElectiveCompleted).count();
        long fullyCompleted = rows.stream()
                .filter(row -> row.isOpenElectiveSelected() && row.isDeptElectiveCompleted())
                .count();

        return new AdminDashboardStatsDTO(registeredStudents, openElectiveTaken, deptElectiveTaken, fullyCompleted);
    }

    public List<AdminStudentRowDTO> getStudentRows() {
        List<UserRepository.StudentRowProjection> students = userRepository.findStudentRows(Role.STUDENT);

        // Active sessions grouped by semester (OPEN + DEPARTMENT)
        Map<Integer, Session> openSessionBySemester = sessionRepository.findByIsActiveTrueAndType(SessionType.OPEN)
                .stream()
                .collect(Collectors.toMap(Session::getSemester, s -> s, (a, b) -> a));
        Map<Integer, Session> deptSessionBySemester = sessionRepository.findByIsActiveTrueAndType(SessionType.DEPARTMENT)
                .stream()
                .collect(Collectors.toMap(Session::getSemester, s -> s, (a, b) -> a));

        // Category count per active DEPARTMENT session
    Map<Long, Integer> categoryCountBySession = deptSessionBySemester.values().stream()
        .collect(Collectors.toMap(Session::getId,
            session -> Math.toIntExact(deptCategoryRepository.countBySession_Id(session.getId())),
            (a, b) -> a));

        // Selection counts grouped by (student, session)
        Map<String, Long> openSelectionCounts = buildSelectionCountMap(
                openElectiveSelectionRepository.countByStudentAndSession());
        Map<String, Long> deptSelectionCounts = buildSelectionCountMap(
                deptElectiveSelectionRepository.countByStudentAndSession());

        List<AdminStudentRowDTO> result = new ArrayList<>();

        for (UserRepository.StudentRowProjection row : students) {
            Long studentId = row.getUserId();
            Integer semester = row.getCurrentSemester() != null ? row.getCurrentSemester() : 0;

            Session openSession = openSessionBySemester.get(semester);
            Session deptSession = deptSessionBySemester.get(semester);

            boolean openSelected = false;
            if (openSession != null) {
                String key = buildKey(studentId, openSession.getId());
                openSelected = openSelectionCounts.getOrDefault(key, 0L) > 0;
            }

            boolean deptCompleted = false;
            if (deptSession != null) {
                int categoryCount = categoryCountBySession.getOrDefault(deptSession.getId(), 0);
                if (categoryCount > 0) {
                    String key = buildKey(studentId, deptSession.getId());
                    long selectedCount = deptSelectionCounts.getOrDefault(key, 0L);
                    deptCompleted = selectedCount >= categoryCount;
                }
            }

            result.add(new AdminStudentRowDTO(
                    studentId,
                    row.getName(),
                    row.getUsn(),
                    row.getDepartment(),
                    semester,
                    row.getEligible() != null && row.getEligible(),
                    openSelected,
                    deptCompleted
            ));
        }

        return result;
    }

    public List<AdminSessionDTO> getSessionOverview() {
    Map<Long, Long> subjectCountBySession = subjectRepository.countSubjectsBySession()
        .stream()
        .collect(Collectors.toMap(
            SubjectRepository.SessionSubjectCountProjection::getSessionId,
            SubjectRepository.SessionSubjectCountProjection::getCount,
            (a, b) -> a));

    return sessionRepository.findAll()
        .stream()
        .map(session -> new AdminSessionDTO(
            session,
            subjectCountBySession.getOrDefault(session.getId(), 0L)))
        .collect(Collectors.toList());
    }

    public List<PopularElectiveDTO> getPopularElectives(int limit) {
    return openElectiveSelectionRepository
        .findPopularOpenElectives(PageRequest.of(0, Math.max(1, limit)))
                .stream()
                .map(p -> new PopularElectiveDTO(
                        p.getSubjectId(),
                        p.getCourseCode(),
                        p.getTitle(),
                        p.getSelectionCount(),
                        p.getFilledSeats(),
                        p.getMaxSeats(),
                        p.getCredits()))
                .collect(Collectors.toList());
    }

    public AdminAnalyticsDTO getAdminAnalytics(int limit) {
    long totalStudents = userRepository.countByRole(Role.STUDENT);
    long eligibleStudents = academicStateRepository.countByIsEligibleTrue();
    long ineligibleStudents = academicStateRepository.countByIsEligibleFalse();
    long openElectiveParticipants = openElectiveSelectionRepository.countDistinctStudents();
    long deptElectiveParticipants = deptElectiveSelectionRepository.countDistinctStudents();

    List<DepartmentCountDTO> departmentCounts = userRepository
        .countStudentsByDepartment(Role.STUDENT)
        .stream()
        .map(row -> new DepartmentCountDTO(
            row.getDepartment() == null ? "" : row.getDepartment(),
            row.getCount() == null ? 0L : row.getCount()))
        .collect(Collectors.toList());

    List<SemesterCountDTO> semesterCounts = academicStateRepository
        .countStudentsBySemester()
        .stream()
        .map(row -> new SemesterCountDTO(
            row.getSemester() == null ? 0 : row.getSemester(),
            row.getCount() == null ? 0L : row.getCount()))
        .collect(Collectors.toList());

    int safeLimit = Math.max(1, limit);
    List<PopularElectiveDTO> openElectivePopular = openElectiveSelectionRepository
        .findPopularOpenElectives(PageRequest.of(0, safeLimit))
        .stream()
        .map(p -> new PopularElectiveDTO(
            p.getSubjectId(),
            p.getCourseCode(),
            p.getTitle(),
            p.getSelectionCount(),
            p.getFilledSeats(),
            p.getMaxSeats(),
            p.getCredits()))
        .collect(Collectors.toList());

    List<PopularElectiveDTO> deptElectivePopular = deptElectiveSelectionRepository
        .findPopularDeptElectives(PageRequest.of(0, safeLimit))
        .stream()
        .map(p -> new PopularElectiveDTO(
            p.getSubjectId(),
            p.getCourseCode(),
            p.getTitle(),
            p.getSelectionCount(),
            p.getFilledSeats(),
            p.getMaxSeats(),
            p.getCredits()))
        .collect(Collectors.toList());


    return new AdminAnalyticsDTO(
        totalStudents,
        eligibleStudents,
        ineligibleStudents,
        openElectiveParticipants,
        deptElectiveParticipants,
        departmentCounts,
        semesterCounts,
        openElectivePopular,
        deptElectivePopular
    );
    }

    private Map<String, Long> buildSelectionCountMap(List<? extends SelectionCountProjection> rows) {
        Map<String, Long> map = new HashMap<>();
        for (SelectionCountProjection row : rows) {
            map.put(buildKey(row.getStudentId(), row.getSessionId()), row.getCount());
        }
        return map;
    }

    private String buildKey(Long studentId, Long sessionId) {
        return studentId + ":" + sessionId;
    }
}
