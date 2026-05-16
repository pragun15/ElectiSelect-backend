package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.DeptElectiveSelection;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeptElectiveSelectionRepository extends JpaRepository<DeptElectiveSelection, Long> {

	@Query("SELECT d.student.id as studentId, d.session.id as sessionId, COUNT(d.id) as count " +
			"FROM DeptElectiveSelection d GROUP BY d.student.id, d.session.id")
	List<SelectionCountProjection> countByStudentAndSession();

	boolean existsByStudentAndSession(User student, Session session);

	// Student Management (admin analytics): any dept elective submission exists for student
	boolean existsByStudent_Id(Long studentId);

	@Query("SELECT COUNT(DISTINCT s.student.id) FROM DeptElectiveSelection s")
	long countDistinctStudents();

	@Query("SELECT subj.id as subjectId, subj.courseCode as courseCode, subj.title as title, " +
			"COUNT(sel.id) as selectionCount, subj.filled_seats as filledSeats, subj.maxSeats as maxSeats, " +
			"subj.credits as credits " +
			"FROM DeptElectiveSelection sel JOIN sel.subject subj " +
			"WHERE (subj.isDeleted = false OR subj.isDeleted IS NULL) " +
			"GROUP BY subj.id, subj.courseCode, subj.title, subj.filled_seats, subj.maxSeats, subj.credits " +
			"ORDER BY COUNT(sel.id) DESC")
	List<PopularElectiveProjection> findPopularDeptElectives(Pageable pageable);

	@Query("SELECT d FROM DeptElectiveSelection d " +
			"JOIN FETCH d.category c " +
			"JOIN FETCH d.subject s " +
			"WHERE d.student = :student AND d.session = :session")
	List<DeptElectiveSelection> findByStudentAndSessionWithDetails(@Param("student") User student,
																@Param("session") Session session);

	interface PopularElectiveProjection {
		Long getSubjectId();
		String getCourseCode();
		String getTitle();
		Long getSelectionCount();
		Integer getFilledSeats();
		Integer getMaxSeats();
		Integer getCredits();
	}
}
