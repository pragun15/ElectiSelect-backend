package com.pragun.ElectiSelect.model;

/**
 * Summary result for bulk semester promotion.
 */
public class PromotionResultDTO {
    private final int requestedSemester;
    private final int promotedCount;
    private final int skippedCount;
    private final String message;

    public PromotionResultDTO(int requestedSemester,
                              int promotedCount,
                              int skippedCount,
                              String message) {
        this.requestedSemester = requestedSemester;
        this.promotedCount = promotedCount;
        this.skippedCount = skippedCount;
        this.message = message;
    }

    public int getRequestedSemester() { return requestedSemester; }
    public int getPromotedCount() { return promotedCount; }
    public int getSkippedCount() { return skippedCount; }
    public String getMessage() { return message; }
}
