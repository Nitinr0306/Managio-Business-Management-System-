package com.nitin.saas.common.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                request.getRequestURI(), ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(),
                request.getRequestURI(), ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(),
                request.getRequestURI(), ErrorCode.DUPLICATE_RESOURCE);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                request.getRequestURI(), ex.getErrorCode());
    }

    @ExceptionHandler(FeatureNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleFeatureNotAvailable(
            FeatureNotAvailableException ex, HttpServletRequest request) {
        log.warn("Feature not available: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ErrorCode.FEATURE_NOT_AVAILABLE.getCode())
                .build();

        if (ex.getFeature() != null) {
            response.addDetail("feature", ex.getFeature());
        }
        if (ex.getRequiredPlan() != null) {
            response.addDetail("requiredPlan", ex.getRequiredPlan());
        }

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleUsageLimitExceeded(
            UsageLimitExceededException ex, HttpServletRequest request) {
        log.warn("Usage limit exceeded: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ErrorCode.USAGE_LIMIT_EXCEEDED.getCode())
                .build();

        if (ex.getResource() != null) {
            response.addDetail("resource", ex.getResource());
            response.addDetail("currentUsage", ex.getCurrentUsage());
            response.addDetail("limit", ex.getLimit());
        }

        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied",
                request.getRequestURI(), ErrorCode.AUTHORIZATION_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials",
                request.getRequestURI(), ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            LockedException ex, HttpServletRequest request) {
        log.warn("Account locked: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.LOCKED, ex.getMessage(),
                request.getRequestURI(), ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {
        log.warn("JWT exception: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token",
                request.getRequestURI(), ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed")
                .path(request.getRequestURI())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .details(errors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        log.warn("Constraint violation: {}", errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Constraint validation failed")
                .path(request.getRequestURI())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .details(errors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'",
                ex.getValue(), ex.getName());
        log.warn("Type mismatch: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message,
                request.getRequestURI(), ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(),
                request.getRequestURI(), ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request.getRequestURI(), ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(BusinessAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleBusinessAlreadyExists(
            BusinessAlreadyExistsException ex,
            HttpServletRequest request) {

        log.warn("Business already exists: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                ErrorCode.DUPLICATE_RESOURCE
        );
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String message, String path, ErrorCode errorCode) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .errorCode(errorCode.getCode())
                .build();
        return new ResponseEntity<>(response, status);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private Integer status;
        private String error;
        private String message;
        private String path;
        private String errorCode;
        private Map<String, Object> details;

        public void addDetail(String key, Object value) {
            if (details == null) {
                details = new HashMap<>();
            }
            details.put(key, value);
        }
    }
}