package com.managio.modules.auth.controller;

import com.managio.modules.auth.service.AuthService;
import com.managio.modules.auth.dto.AuthDtos.GoogleSignInRequest;
import com.managio.modules.auth.dto.AuthDtos.PasswordResetConfirmRequest;
import com.managio.modules.auth.dto.AuthDtos.PasswordResetRequest;
import com.managio.modules.auth.dto.AuthDtos.SessionResponse;
import com.managio.modules.auth.dto.AuthDtos.SignInRequest;
import com.managio.modules.auth.dto.AuthDtos.SignUpRequest;
import com.managio.modules.auth.dto.AuthDtos.UserResponse;
import com.managio.modules.auth.dto.AuthDtos.VerifyEmailRequest;
import com.managio.modules.auth.service.AuthService.SignedIn;
import com.managio.platform.security.RefreshCookies;
import com.managio.platform.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public sign-in endpoints for owners and team members. Refresh tokens only travel in the {@code mg_rt} cookie. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService auth;
    private final RefreshCookies cookies;

    @PostMapping("/sign-up")
    ResponseEntity<SessionResponse> signUp(@Valid @RequestBody SignUpRequest request, HttpServletRequest http) {
        return newSession(HttpStatus.CREATED, auth.signUp(request, http), http);
    }

    @PostMapping("/sign-in")
    ResponseEntity<SessionResponse> signIn(@Valid @RequestBody SignInRequest request, HttpServletRequest http) {
        return newSession(HttpStatus.OK, auth.signIn(request, http), http);
    }

    @PostMapping("/google")
    ResponseEntity<SessionResponse> google(@Valid @RequestBody GoogleSignInRequest request, HttpServletRequest http) {
        return newSession(HttpStatus.OK, auth.signInWithGoogle(request.credential(), http), http);
    }

    @PostMapping("/refresh")
    ResponseEntity<SessionResponse> refresh(HttpServletRequest http, HttpServletResponse response) {
        try {
            String token = cookies.read(http, RefreshCookies.USER).orElseThrow(() ->
                    ApiException.unauthorized("auth.session_expired", "Your session has ended. Sign in again."));
            return session(HttpStatus.OK, auth.refresh(token, http));
        } catch (ApiException e) {
            if (e.status() == HttpStatus.UNAUTHORIZED) {
                // A dead session: drop the cookie so the browser stops sending it. The error body is written as usual.
                response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear(RefreshCookies.USER).toString());
            }
            throw e;
        }
    }

    @PostMapping("/sign-out")
    ResponseEntity<Void> signOut(HttpServletRequest http) {
        auth.signOut(cookies.read(http, RefreshCookies.USER).orElse(null));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear(RefreshCookies.USER).toString())
                .build();
    }

    @PostMapping("/verify-email")
    UserResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return UserResponse.from(auth.verifyEmail(request.token()));
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest http) {
        auth.requestPasswordReset(request.email(), http);
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        auth.resetPassword(request.token(), request.password());
    }

    /**
     * A successful sign-in replaces the browser's refresh cookie, so the session that cookie belonged to is ended
     * rather than left valid and unused for its remaining 30 days. A failed sign-in reaches neither line.
     */
    private ResponseEntity<SessionResponse> newSession(HttpStatus status, SignedIn signedIn, HttpServletRequest http) {
        cookies.read(http, RefreshCookies.USER).ifPresent(auth::signOut);
        return session(status, signedIn);
    }

    private ResponseEntity<SessionResponse> session(HttpStatus status, SignedIn signedIn) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.issue(RefreshCookies.USER, signedIn.refreshToken()).toString())
                .body(new SessionResponse(signedIn.accessToken().value(), signedIn.accessToken().expiresInSeconds(),
                        UserResponse.from(signedIn.user())));
    }
}
