package com.nitin.saas.common.exception;

/**
 * Thrown when a user account is locked due to too many failed login attempts.
 *
 * ROOT CAUSE FIX for accountLockingJourney test (401 instead of 423):
 *   Spring Security's {@code LockedException} extends {@code AuthenticationException}.
 *   When thrown from a service method, {@code ExceptionTranslationFilter} intercepts it
 *   BEFORE {@code GlobalExceptionHandler} gets a chance to act, and delegates to
 *   {@code JwtAuthenticationEntryPoint} which always returns 401 — not 423.
 *
 *   This class extends plain {@code RuntimeException} (NOT Spring Security's
 *   {@code AuthenticationException}), so Spring Security's filter chain does NOT
 *   intercept it. It propagates normally to {@code GlobalExceptionHandler} which
 *   returns HTTP 423 Locked.
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(String message) {
        super(message);
    }

    public AccountLockedException(String message, Throwable cause) {
        super(message, cause);
    }
}