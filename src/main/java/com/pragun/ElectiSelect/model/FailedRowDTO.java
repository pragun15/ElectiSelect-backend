package com.pragun.ElectiSelect.model;

/**
 * Failed row info for bulk student import.
 */
public class FailedRowDTO {
    private final int rowNumber;
    private final String reason;

    public FailedRowDTO(int rowNumber, String reason) {
        this.rowNumber = rowNumber;
        this.reason = reason;
    }

    public int getRowNumber() { return rowNumber; }
    public String getReason() { return reason; }
}
