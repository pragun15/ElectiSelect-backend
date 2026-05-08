package com.pragun.ElectiSelect.repository;

import com.pragun.ElectiSelect.model.DeptCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeptCategoryRepository extends JpaRepository<DeptCategory, Long> {

    /**
     * Fetch all categories that belong to the given session.
     * Used when building the dept-elective response grouped by category.
     */
    List<DeptCategory> findBySession_Id(Long sessionId);
}
