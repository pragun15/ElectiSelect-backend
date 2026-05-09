package com.pragun.ElectiSelect.model;

import java.util.List;

/**
 * Response for GET /api/electives/dept/my-selection.
 */
public class DeptElectiveSelectionStatusResponse {
    private final boolean submitted;
    private final Long sessionId;
    private final List<DeptElectiveSelectionSummary> selections;

    public DeptElectiveSelectionStatusResponse(boolean submitted,
                                               Long sessionId,
                                               List<DeptElectiveSelectionSummary> selections) {
        this.submitted = submitted;
        this.sessionId = sessionId;
        this.selections = selections;
    }

    public boolean isSubmitted() { return submitted; }
    public Long getSessionId() { return sessionId; }
    public List<DeptElectiveSelectionSummary> getSelections() { return selections; }
}
