package com.managio.modules.team.controller;

import com.managio.modules.auth.service.AuthService;
import com.managio.modules.team.service.InvitationService;
import com.managio.modules.auth.dto.AuthDtos.SessionResponse;
import com.managio.modules.auth.dto.AuthDtos.UserResponse;
import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.platform.security.RefreshCookies;
import com.managio.modules.team.service.InvitationService.Accepted;
import com.managio.modules.team.service.InvitationService.InvitationPreview;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Opening and accepting an invitation link; works before the invitee has an account. */
@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
class PublicInvitationController {

    private final InvitationService invitations;
    private final AuthService auth;
    private final RefreshCookies cookies;

    record TokenRequest(@NotBlank String token) {
    }

    /** New invitees send a name and password, or a Google credential; signed-in invitees send only the token. */
    record AcceptRequest(@NotBlank String token, @Size(max = 150) String fullName,
                         @Size(max = 72, message = "Use 72 characters or fewer") String password,
                         String googleCredential) {
    }

    /** {@code session} is present when accepting signed the invitee in. */
    record AcceptResponse(long businessId, SessionResponse session) {
    }

    @PostMapping("/preview")
    InvitationPreview preview(@Valid @RequestBody TokenRequest request) {
        return invitations.preview(request.token());
    }

    @PostMapping("/accept")
    ResponseEntity<AcceptResponse> accept(@AuthenticationPrincipal UserPrincipal principal,
                                          @Valid @RequestBody AcceptRequest request, HttpServletRequest http) {
        Accepted accepted = invitations.accept(request.token(), principal, request.fullName(), request.password(),
                request.googleCredential(), http);
        if (accepted.signedIn() == null) {
            return ResponseEntity.ok(new AcceptResponse(accepted.businessId(), null));
        }
        var signedIn = accepted.signedIn();
        // The new account's session replaces whatever session the browser's cookie belonged to.
        cookies.read(http, RefreshCookies.USER).ifPresent(auth::signOut);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.issue(RefreshCookies.USER, signedIn.refreshToken()).toString())
                .body(new AcceptResponse(accepted.businessId(), new SessionResponse(signedIn.accessToken().value(),
                        signedIn.accessToken().expiresInSeconds(), UserResponse.from(signedIn.user()))));
    }
}
