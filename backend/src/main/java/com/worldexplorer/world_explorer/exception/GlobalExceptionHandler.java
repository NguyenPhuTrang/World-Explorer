// quản lý và xử lý lỗi tập trung cho toàn bộ Backend
package com.worldexplorer.world_explorer.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle authentication-related IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(
            IllegalArgumentException exception
    ) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage()
        );
    }

    // Handle exceptions with explicitly defined HTTP status
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(
            ResponseStatusException exception
    ) {
        return buildErrorResponse(
                exception.getStatusCode(),
                exception.getReason()
        );
    }

    // Handle request validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Request validation failed");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    // Handle parameter validation errors
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    // Handle malformed JSON and invalid request parameter formats
    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, String>> handleInvalidRequest(
            Exception exception
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request format or missing required parameter"
        );
    }

    // Handle access denied errors
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource"
        );
    }

    // Build standardized error response
    private ResponseEntity<Map<String, String>> buildErrorResponse(
            HttpStatusCode status,
            String message
    ) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());

        String errorCode = httpStatus != null
                ? httpStatus.name()
                : "HTTP_ERROR";

        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", errorCode,
                        "message", message != null
                                ? message
                                : "An unexpected error occurred"
                ));
    }
}
