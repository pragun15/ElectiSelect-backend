package com.pragun.ElectiSelect.util;

public class ValidationUtils {

    public static void validateAcademicYear(String academicYear) {
        if (academicYear == null || !academicYear.matches("^\\d{4}-\\d{4}$")) {
            throw new IllegalArgumentException("VALIDATION_FAILED:Academic year must be in YYYY-YYYY format (e.g. 2024-2025).");
        }

        int firstYear = Integer.parseInt(academicYear.substring(0, 4));
        int secondYear = Integer.parseInt(academicYear.substring(5, 9));

        if (secondYear != firstYear + 1) {
            throw new IllegalArgumentException("VALIDATION_FAILED:Second year must be exactly first year + 1 (e.g. 2024-2025).");
        }

        if (firstYear < 2020 || firstYear > 2035) {
            throw new IllegalArgumentException("VALIDATION_FAILED:Academic year must be between 2020 and 2035.");
        }
    }
}
