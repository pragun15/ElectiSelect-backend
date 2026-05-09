package com.pragun.ElectiSelect.repository;

/**
 * Projection for (student_id, session_id) selection counts.
 */
public interface SelectionCountProjection {
    Long getStudentId();
    Long getSessionId();
    Long getCount();
}
