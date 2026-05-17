package com.pragun.ElectiSelect.model;

import java.util.List;

public class SubjectUploadPreviewDTO {
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<SubjectUploadRowDTO> validSubjects;
    private List<SubjectUploadErrorDTO> invalidSubjects;

    public SubjectUploadPreviewDTO() {}

    public SubjectUploadPreviewDTO(int totalRows,
                                   int validRows,
                                   int invalidRows,
                                   List<SubjectUploadRowDTO> validSubjects,
                                   List<SubjectUploadErrorDTO> invalidSubjects) {
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.invalidRows = invalidRows;
        this.validSubjects = validSubjects;
        this.invalidSubjects = invalidSubjects;
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }
    public int getInvalidRows() { return invalidRows; }
    public void setInvalidRows(int invalidRows) { this.invalidRows = invalidRows; }
    public List<SubjectUploadRowDTO> getValidSubjects() { return validSubjects; }
    public void setValidSubjects(List<SubjectUploadRowDTO> validSubjects) { this.validSubjects = validSubjects; }
    public List<SubjectUploadErrorDTO> getInvalidSubjects() { return invalidSubjects; }
    public void setInvalidSubjects(List<SubjectUploadErrorDTO> invalidSubjects) { this.invalidSubjects = invalidSubjects; }
}
