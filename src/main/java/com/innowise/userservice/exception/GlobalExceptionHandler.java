package com.innowise.userservice.exception;

import com.innowise.userservice.model.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler({UserNotFoundException.class, PaymentCardNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, WebRequest request) {
        return buildResponse(
                HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), extractPath(request), null);
    }

    @ExceptionHandler({DuplicateEmailException.class, DuplicateCardNumberException.class})
    public ResponseEntity<ErrorResponse> handleConflict(Exception ex, WebRequest request) {
        return buildResponse(
                HttpStatus.CONFLICT, "Conflict", ex.getMessage(), extractPath(request), null);
    }

    @ExceptionHandler({
            ActiveCardDeletionException.class,
            UserDeactivationNotAllowedException.class,
            UserDeletionNotAllowedException.class,
            MaxPaymentCardsLimitException.class
    })
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage(),
                extractPath(request), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String key = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            errors.put(key, error.getDefaultMessage());
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", "Invalid request data",
                extractPath(request), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed JSON request",
                extractPath(request),
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Invalid parameter value",
                extractPath(request),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred", extractPath(request), null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, String path,
                                                        Map<String, String> validationErrors) {
        ErrorResponse response = ErrorResponse.builder()
            .timestamp(Instant.now(clock))
            .status(status.value())
            .error(error)
            .message(message)
            .validationErrors(validationErrors)
            .path(path)
            .build();
        return new ResponseEntity<>(response, status);
    }

    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
