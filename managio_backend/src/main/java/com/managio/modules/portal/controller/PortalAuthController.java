package com.managio.modules.portal.controller;

import com.managio.modules.portal.service.PortalAuthService;
import com.managio.platform.security.RefreshCookies;
import com.managio.platform.error.ApiException;
import com.managio.modules.portal.service.PortalAuthService.SignedIn;
import com.managio.modules.portal.dto.PortalDtos.BusinessPreview;
import com.managio.modules.portal.dto.PortalDtos.PortalSession;
import com.managio.modules.portal.dto.PortalDtos.ResetBody;
import com.managio.modules.portal.dto.PortalDtos.SetPasswordBody;
import com.managio.modules.portal.dto.PortalDtos.SignInBody;
import com.managio.modules.portal.dto.PortalDtos.SignUpBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public customer sign-in endpoints. The refresh token only travels in the {@code mg_prt} cookie. */
@RestController
@RequestMapping("/api/v1/portal/auth")
@RequiredArgsConstructor
class PortalAuthController {

    private final PortalAuthService auth;
    private final RefreshCookies cookies;

    @GetMapping("/business/{code}")
    BusinessPreview business(@PathVariable String code) {
        return auth.preview(code);
    }

    @PostMapping("/sign-in")
    ResponseEntity<PortalSession> signIn(@Valid @RequestBody SignInBody body, HttpServletRequest http) {
        return newSession(HttpStatus.OK, auth.signIn(body, http), http);
    }

    @PostMapping("/sign-up")
    ResponseEntity<PortalSession> signUp(@Valid @RequestBody SignUpBody body, HttpServletRequest http) {
        return newSession(HttpStatus.CREATED, auth.signUp(body, http), http);
    }

    @PostMapping("/set-password")
    ResponseEntity<PortalSession> setPassword(@Valid @RequestBody SetPasswordBody body, HttpServletRequest http) {
        return newSession(HttpStatus.OK, auth.setPassword(body.token(), body.password(), http), http);
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestReset(@Valid @RequestBody ResetBody body, HttpServletRequest http) {
        auth.requestReset(body.businessCode(), body.identifier(), http);
    }

    @PostMapping("/refresh")
    ResponseEntity<PortalSession> refresh(HttpServletRequest http, HttpServletResponse response) {
        try {
            String token = cookies.read(http, RefreshCookies.CUSTOMER).orElseThrow(() ->
                    ApiException.unauthorized("auth.session_expired", "Your session has ended. Sign in again."));
            return session(HttpStatus.OK, auth.refresh(token, http));
        } catch (ApiException e) {
            if (e.status() == HttpStatus.UNAUTHORIZED) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear(RefreshCookies.CUSTOMER).toString());
            }
            throw e;
        }
    }

    @PostMapping("/sign-out")
    ResponseEntity<Void> signOut(HttpServletRequest http) {
        auth.signOut(cookies.read(http, RefreshCookies.CUSTOMER).orElse(null));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear(RefreshCookies.CUSTOMER).toString())
                .build();
    }

    static ResponseEntity<PortalSession> session(HttpStatus status, SignedIn signedIn, RefreshCookies cookies) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookies.issue(RefreshCookies.CUSTOMER, signedIn.refreshToken()).toString())
                .body(new PortalSession(signedIn.accessToken().value(), signedIn.accessToken().expiresInSeconds(),
                        signedIn.summary(), signedIn.business()));
    }

    /** Like staff sign-in: the session behind the cookie this one replaces ends instead of lingering. */
    private ResponseEntity<PortalSession> newSession(HttpStatus status, SignedIn signedIn, HttpServletRequest http) {
        cookies.read(http, RefreshCookies.CUSTOMER).ifPresent(auth::signOut);
        return session(status, signedIn, cookies);
    }

    private ResponseEntity<PortalSession> session(HttpStatus status, SignedIn signedIn) {
        return session(status, signedIn, cookies);
    }
}
