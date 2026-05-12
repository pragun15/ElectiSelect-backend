package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.DeptElectiveSelection;
import com.pragun.ElectiSelect.model.Session;
import com.pragun.ElectiSelect.model.User;
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

	@Query("SELECT d FROM DeptElectiveSelection d " +
			"JOIN FETCH d.category c " +
			"JOIN FETCH d.subject s " +
			"WHERE d.student = :student AND d.session = :session")
	List<DeptElectiveSelection> findByStudentAndSessionWithDetails(@Param("student") User student,
																@Param("session") Session session);
}
