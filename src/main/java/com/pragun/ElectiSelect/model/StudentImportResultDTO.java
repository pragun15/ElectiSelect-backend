package com.pragun.ElectiSelect.model;

import java.util.List;

/**
 * Summary result for bulk student import.
 */
public class StudentImportResultDTO {
    private final int totalRows;
    private final int importedCount;
    private final int skippedCount;
    private final List<FailedRowDTO> failedRows;
    private final String message;

    public StudentImportResultDTO(int totalRows,
                                 int importedCount,
                                 int skippedCount,
                                 List<FailedRowDTO> failedRows,
                                 String message) {
        this.totalRows = totalRows;
        this.importedCount = importedCount;
        this.skippedCount = skippedCount;
        this.failedRows = failedRows;
        this.message = message;
    }

    public int getTotalRows() { return totalRows; }
    public int getImportedCount() { return importedCount; }
    public int getSkippedCount() { return skippedCount; }
    public List<FailedRowDTO> getFailedRows() { return failedRows; }
    public String getMessage() { return message; }
}
