package com.pragun.ElectiSelect.model;

import java.util.List;

public class SubjectUploadConfirmResultDTO {
    private int createdCount;
    private int skippedCount;
    private List<String> skippedCourseCodes;

    public SubjectUploadConfirmResultDTO() {}

    public SubjectUploadConfirmResultDTO(int createdCount, int skippedCount, List<String> skippedCourseCodes) {
        this.createdCount = createdCount;
        this.skippedCount = skippedCount;
        this.skippedCourseCodes = skippedCourseCodes;
    }

    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public List<String> getSkippedCourseCodes() { return skippedCourseCodes; }
    public void setSkippedCourseCodes(List<String> skippedCourseCodes) { this.skippedCourseCodes = skippedCourseCodes; }
}
