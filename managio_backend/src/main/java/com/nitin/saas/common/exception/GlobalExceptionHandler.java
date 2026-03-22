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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Domain exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex,
                                                          HttpServletRequest req) {
        log.warn("Bad request [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest req) {
        log.warn("Not found [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex,
                                                        HttpServletRequest req) {
        log.warn("Conflict [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, ErrorCode.DUPLICATE_RESOURCE);
    }

    @ExceptionHandler(BusinessAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleBusinessExists(BusinessAlreadyExistsException ex,
                                                              HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, ErrorCode.DUPLICATE_RESOURCE);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest req) {

        log.warn("Business exception [{}]: {}", req.getRequestURI(), ex.getMessage());

        HttpStatus status;

        // ✅ AUTH-related errors → 401
        if (
                ex.getErrorCode() == ErrorCode.EMAIL_NOT_VERIFIED ||
                        ex.getErrorCode() == ErrorCode.INVALID_CREDENTIALS
        ) {
            status = HttpStatus.UNAUTHORIZED;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        return build(status, ex.getMessage(), req, ex.getErrorCode());
    }

    @ExceptionHandler(FeatureNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleFeature(FeatureNotAvailableException ex,
                                                       HttpServletRequest req) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .errorCode(ErrorCode.FEATURE_NOT_AVAILABLE.getCode())
                .build();
        if (ex.getFeature() != null)     response.addDetail("feature",     ex.getFeature());
        if (ex.getRequiredPlan() != null) response.addDetail("requiredPlan", ex.getRequiredPlan());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleUsageLimit(UsageLimitExceededException ex,
                                                          HttpServletRequest req) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .errorCode(ErrorCode.USAGE_LIMIT_EXCEEDED.getCode())
                .build();
        if (ex.getResource() != null) {
            response.addDetail("resource",     ex.getResource());
            response.addDetail("currentUsage", ex.getCurrentUsage());
            response.addDetail("limit",        ex.getLimit());
        }
        return new ResponseEntity<>(response, HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── Security exceptions ───────────────────────────────────────────────────

    /**
     * FIX: AccountLockedException extends RuntimeException (NOT Spring's LockedException)
     * so it reaches this handler instead of being swallowed by ExceptionTranslationFilter
     * → returns HTTP 423 Locked.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex,
                                                             HttpServletRequest req) {
        log.warn("Account locked [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.LOCKED, ex.getMessage(), req, ErrorCode.ACCOUNT_LOCKED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleSpringLocked(LockedException ex,
                                                            HttpServletRequest req) {
        log.warn("Spring LockedException [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.LOCKED, ex.getMessage(), req, ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest req) {
        log.warn("Access denied [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access denied", req, ErrorCode.AUTHORIZATION_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                              HttpServletRequest req) {
        log.warn("Bad credentials [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, ErrorCode.AUTHENTICATION_ERROR);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwt(JwtException ex, HttpServletRequest req) {
        log.warn("JWT exception [{}]: {}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid or expired token",
                req, ErrorCode.AUTHENTICATION_ERROR);
    }

    // ── Validation exceptions ─────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e -> {
            String field   = ((FieldError) e).getField();
            String message = e.getDefaultMessage();
            errors.put(field, message);
        });
        log.warn("Validation failed [{}]: {}", req.getRequestURI(), errors);
        ErrorResponse r = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Input validation failed. Check the 'details' field.")
                .path(req.getRequestURI())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .details(errors)
                .build();
        return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex,
                                                          HttpServletRequest req) {
        Map<String, Object> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a));
        ErrorResponse r = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Constraint validation failed")
                .path(req.getRequestURI())
                .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
                .details(errors)
                .build();
        return new ResponseEntity<>(r, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body",
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing",
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type '" + ex.getContentType() + "' is not supported",
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file exceeds the maximum allowed size (10 MB)",
                req, ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, ErrorCode.VALIDATION_ERROR);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                req, ErrorCode.INTERNAL_ERROR);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest req, ErrorCode code) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .errorCode(code.getCode())
                .build();
        return new ResponseEntity<>(body, status);
    }

    // ── Response DTO ──────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorResponse {
        private LocalDateTime       timestamp;
        private Integer             status;
        private String              error;
        private String              message;
        private String              path;
        private String              errorCode;
        private Map<String, Object> details;

        public void addDetail(String key, Object value) {
            if (details == null) details = new HashMap<>();
            details.put(key, value);
        }
    }
}