package com.managio.platform.error;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Turns every error into a problem detail with a stable {@code code} (docs/API.md#errors). */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        ProblemDetail problem = Problems.of(ex.status(), ex.code(), titleFor(ex.status()), ex.getMessage());
        return ResponseEntity.status(ex.status()).body(Problems.withErrors(problem, ex.fieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> {
            String path = v.getPropertyPath().toString();
            errors.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
        });
        return validationProblem(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Problems.of(HttpStatus.FORBIDDEN, "auth.forbidden",
                "Not allowed", "You don't have permission to do this."));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleStale(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Problems.of(HttpStatus.CONFLICT, "conflict.stale",
                "Changed by someone else", "Someone else changed this at the same time. Reload and try again."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Problems.of(HttpStatus.CONFLICT, "conflict.integrity",
                "Conflicts with existing data", "This conflicts with existing data. Reload and try again."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Problems.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "server.error", "Something went wrong", "Something went wrong on our side. Try again in a moment."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors().forEach(e -> errors.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationBody(errors));
    }

    /** Framework errors (bad JSON, wrong method, missing parameter) keep their status and get a code. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        String detail = statusCode.value() == 400
                ? "The request could not be read. Check the values you sent."
                : HttpStatus.valueOf(statusCode.value()).getReasonPhrase() + ".";
        ProblemDetail problem = Problems.of(statusCode, "request." + statusCode.value(), titleFor(statusCode), detail);
        return ResponseEntity.status(statusCode).headers(headers).body(problem);
    }

    private ResponseEntity<ProblemDetail> validationProblem(Map<String, String> errors) {
        return ResponseEntity.badRequest().body(validationBody(errors));
    }

    private static ProblemDetail validationBody(Map<String, String> errors) {
        String detail = errors.size() == 1 ? "1 field needs attention." : errors.size() + " fields need attention.";
        return Problems.withErrors(
                Problems.of(HttpStatus.BAD_REQUEST, "validation.failed", "Check the highlighted fields", detail), errors);
    }

    private static String titleFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Check the request";
            case 401 -> "Sign in required";
            case 403 -> "Not allowed";
            case 404 -> "Not found";
            case 409 -> "Conflict";
            case 422 -> "Can't do that";
            case 429 -> "Too many attempts";
            default -> HttpStatus.valueOf(status.value()).getReasonPhrase();
        };
    }
}
