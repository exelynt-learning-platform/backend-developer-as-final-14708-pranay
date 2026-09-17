package com.example.resourcebooking.common;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({ForbiddenException.class, org.springframework.security.access.AccessDeniedException.class})
    ResponseEntity<ApiError> forbidden(Exception ex) {
        return error(HttpStatus.FORBIDDEN, "Access denied", List.of(ex.getMessage()));
    }

    @ExceptionHandler({BusinessRuleException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> badRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformedBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or invalid request body", List.of(ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> typeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter: " + ex.getName();
        return error(HttpStatus.BAD_REQUEST, message, List.of(String.valueOf(ex.getValue())));
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    ResponseEntity<ApiError> unauthorized(Exception ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return error(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> generic(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", List.of(ex.getMessage()));
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), status.getReasonPhrase(), message, details));
    }
}
