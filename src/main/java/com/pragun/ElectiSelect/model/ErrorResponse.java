package com.pragun.ElectiSelect.model;

/**
 * Structured error response — workflow.md §6 API Response Standards.
 * Every error returns: { "error": true, "code": "ERROR_CODE", "message": "..." }
 */
public class ErrorResponse {

    private final boolean error = true;
    private final String code;
    private final String message;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isError()    { return error; }
    public String  getCode()    { return code; }
    public String  getMessage() { return message; }
}
