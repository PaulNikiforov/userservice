package com.innowise.userservice.model.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for API exceptions.
 *
 * @param timestamp    When the error occurred
 * @param status       HTTP status code
 * @param error        HTTP error description
 * @param message      Detailed error message
 * @param validationErrors Field-level validation errors (optional, for 400 Bad Request)
 * @param path         Request URI that caused the error
 */
@Builder
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    Map<String, String> validationErrors,
    String path
) {
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, null, path);
    }
}
