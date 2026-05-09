package com.pragun.ElectiSelect.controller;

import com.pragun.ElectiSelect.model.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Set;

/**
 * Converts RuntimeExceptions thrown by any @RestController into structured JSON error responses.
 * Format: { "error": true, "code": "ERROR_CODE", "message": "..." } — workflow.md §6.
 *
 * Only catches exceptions that propagate out of controllers.
 * Controllers that still have their own try-catch blocks (e.g. AdminController.uploadSubjects)
 * will continue to handle those themselves until those are migrated.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // All stable error codes defined in workflow.md §6
    private static final Set<String> KNOWN_CODES = Set.of(
            "ALREADY_SELECTED",
            "NO_SEATS_AVAILABLE",
            "SESSION_INVALID",
            "NOT_ELIGIBLE",
            "DEPARTMENT_RESTRICTED",
            "SESSION_ALREADY_ACTIVE",
        "SESSION_ALREADY_EXISTS",
        "SUBJECT_UNAVAILABLE",
        "VALIDATION_FAILED"
    );

    // Human-readable messages paired to each code
    private static final Map<String, String> CODE_MESSAGES = Map.of(
            "ALREADY_SELECTED",       "You have already made a selection for this session.",
            "NO_SEATS_AVAILABLE",     "No seats are available for this subject.",
            "SESSION_INVALID",        "The selection session is not currently valid for your semester.",
            "NOT_ELIGIBLE",           "You are not eligible to make a selection in this session.",
            "DEPARTMENT_RESTRICTED",  "Your department is not permitted to select this subject.",
            "SESSION_ALREADY_ACTIVE", "An active session of this type already exists.",
        "SESSION_ALREADY_EXISTS", "Session already exists for this type, semester, and academic year.",
        "SUBJECT_UNAVAILABLE",    "This subject is no longer available.",
        "VALIDATION_FAILED",      "Validation failed."
    );

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        String raw = ex.getMessage() != null ? ex.getMessage() : "";

    if (raw.startsWith("VALIDATION_FAILED:")) {
        String message = raw.substring("VALIDATION_FAILED:".length()).trim();
        return ResponseEntity
            .status(400)
            .body(new ErrorResponse("VALIDATION_FAILED", message.isBlank() ? "Validation failed." : message));
    }

    if (KNOWN_CODES.contains(raw)) {
            String friendly = CODE_MESSAGES.getOrDefault(raw, raw);
            return ResponseEntity
                    .status(resolveHttpStatus(raw))
                    .body(new ErrorResponse(raw, friendly));
        }

        // Spring Security AccessDeniedException
        if (ex instanceof org.springframework.security.access.AccessDeniedException) {
            return ResponseEntity
                    .status(403)
                    .body(new ErrorResponse("ACCESS_DENIED", "You do not have permission to access this resource."));
        }

        // Log the unrecognised exception so we can debug it
        System.err.println("🔥 UNHANDLED EXCEPTION: ");
        ex.printStackTrace();

        // Unrecognised exception — 500, but temporarily leak message for debugging
        return ResponseEntity
                .status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "Unexpected error: " + raw));
    }

    private int resolveHttpStatus(String code) {
        return switch (code) {
            case "ALREADY_SELECTED",
                 "NO_SEATS_AVAILABLE",
              "SESSION_ALREADY_EXISTS",
                 "SESSION_ALREADY_ACTIVE"  -> 409;
            case "SESSION_INVALID",
                 "NOT_ELIGIBLE",
                 "DEPARTMENT_RESTRICTED"   -> 403;
            default                        -> 400;
        };
    }
}
