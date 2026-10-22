package com.managio.modules.auth.service;

import com.managio.modules.auth.domain.SignInEvent;
import com.managio.modules.auth.domain.SubjectType;
import com.managio.modules.auth.repository.SignInEventRepository;
import com.managio.modules.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Records sign-in outcomes and lockouts in their own transaction, so they persist even though the failed
 * sign-in request itself ends with an error (fixes v1 audit finding 6).
 */
@Component
@RequiredArgsConstructor
public class SignInAttempts {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCKOUT = Duration.ofMinutes(15);

    private final UserRepository users;
    private final SignInEventRepository events;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failedUserSignIn(Long userId, String identifier, String detail, HttpServletRequest request) {
        Instant now = clock.instant();
        if (userId != null) {
            users.findById(userId).ifPresent(u -> u.recordFailedSignIn(now, MAX_FAILED_ATTEMPTS, LOCKOUT));
        }
        events.save(event(SubjectType.USER, userId, identifier, "FAILURE", detail, request, now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SubjectType type, Long subjectId, String identifier, String outcome, String detail,
                       HttpServletRequest request) {
        events.save(event(type, subjectId, identifier, outcome, detail, request, clock.instant()));
    }

    private static SignInEvent event(SubjectType type, Long subjectId, String identifier, String outcome, String detail,
                                     HttpServletRequest request, Instant now) {
        return new SignInEvent(type, subjectId, identifier, outcome, detail,
                request == null ? null : request.getRemoteAddr(),
                request == null ? null : request.getHeader("User-Agent"), now);
    }
}
