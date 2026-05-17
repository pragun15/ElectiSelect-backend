package com.pragun.ElectiSelect.model;

import java.util.List;

public class SubjectUploadConfirmRequestDTO {
    private Long sessionId;
    private List<SubjectUploadRowDTO> subjects;

    public SubjectUploadConfirmRequestDTO() {}

    public SubjectUploadConfirmRequestDTO(Long sessionId, List<SubjectUploadRowDTO> subjects) {
        this.sessionId = sessionId;
        this.subjects = subjects;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public List<SubjectUploadRowDTO> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectUploadRowDTO> subjects) { this.subjects = subjects; }
}
