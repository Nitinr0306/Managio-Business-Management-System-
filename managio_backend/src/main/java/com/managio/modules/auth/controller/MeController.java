package com.managio.modules.auth.controller;

import com.managio.modules.auth.service.AuthService;
import com.managio.modules.auth.dto.AuthDtos.ChangePasswordRequest;
import com.managio.modules.auth.dto.AuthDtos.SessionResponse;
import com.managio.modules.auth.dto.AuthDtos.UpdateProfileRequest;
import com.managio.modules.auth.dto.AuthDtos.UserResponse;
import com.managio.modules.auth.service.AuthService.SignedIn;
import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.platform.security.RefreshCookies;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own account. Their businesses are listed by {@code GET /api/v1/me/businesses}. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
class MeController {

    private final AuthService auth;
    private final RefreshCookies cookies;

    @GetMapping
    UserResponse me(@AuthenticationPrincipal UserPrincipal user) {
        return UserResponse.from(auth.get(user.id()));
    }

    @PatchMapping
    UserResponse update(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(auth.updateProfile(user.id(), request.fullName(), request.phone()));
    }

    @PostMapping("/password")
    ResponseEntity<SessionResponse> changePassword(@AuthenticationPrincipal UserPrincipal user,
                                                   @Valid @RequestBody ChangePasswordRequest request,
                                                   HttpServletRequest http) {
        SignedIn signedIn = auth.changePassword(user.id(), request, http);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.issue(RefreshCookies.USER, signedIn.refreshToken()).toString())
                .body(new SessionResponse(signedIn.accessToken().value(), signedIn.accessToken().expiresInSeconds(),
                        UserResponse.from(signedIn.user())));
    }

    @PostMapping("/verification-email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(@AuthenticationPrincipal UserPrincipal user, HttpServletRequest http) {
        auth.resendVerificationEmail(user.id(), http);
    }
}
