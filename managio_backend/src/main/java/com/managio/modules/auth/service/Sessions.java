package com.managio.modules.auth.service;

import com.managio.modules.auth.domain.RefreshToken;
import com.managio.modules.auth.domain.SubjectType;
import com.managio.modules.auth.repository.RefreshTokenRepository;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.security.SecureTokens;
import com.managio.platform.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh-token sessions for users and customers (D-009, IDN-5).
 *
 * <p>Every refresh rotates the token. Presenting an already-rotated token is treated as theft and revokes the
 * whole family, except within a short grace period after rotation: two browser tabs refreshing at the same
 * moment would otherwise sign the person out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Sessions {

    static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private final RefreshTokenRepository tokens;
    private final ManagioProperties properties;
    private final Clock clock;

    /** A refreshed session: whose it is and the new raw refresh token to put in the cookie. */
    public record Refreshed(long subjectId, String refreshToken) {
    }

    /** Starts a new token family and returns the raw refresh token. */
    @Transactional
    public String start(SubjectType type, long subjectId, HttpServletRequest request) {
        return issue(type, subjectId, UUID.randomUUID(), request);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Refreshed refresh(SubjectType type, String rawToken, HttpServletRequest request) {
        Instant now = clock.instant();
        RefreshToken current = tokens.findByTokenHash(SecureTokens.hash(rawToken))
                .filter(t -> t.getSubjectType() == type)
                .orElseThrow(Sessions::expired);
        if (current.getRevokedAt() != null || current.getExpiresAt().isBefore(now)) {
            throw expired();
        }
        if (current.getRotatedAt() != null) {
            if (current.getRotatedAt().plus(REUSE_GRACE).isBefore(now)) {
                log.warn("Refresh token reuse detected; revoking family for {} {}", type, current.getSubjectId());
                tokens.revokeFamily(current.getFamilyId(), now);
                throw expired();
            }
        } else {
            current.markRotated(now);
        }
        return new Refreshed(current.getSubjectId(),
                issue(type, current.getSubjectId(), current.getFamilyId(), request));
    }

    /** Signs out the session the token belongs to. Unknown tokens are ignored. */
    @Transactional
    public void end(String rawToken) {
        tokens.findByTokenHash(SecureTokens.hash(rawToken))
                .ifPresent(t -> tokens.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    /** Signs the subject out everywhere, e.g. after a password change or reset. */
    @Transactional
    public void endAll(SubjectType type, long subjectId) {
        tokens.revokeAll(type, subjectId, clock.instant());
    }

    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return tokens.deleteExpiredBefore(cutoff);
    }

    private String issue(SubjectType type, long subjectId, UUID family, HttpServletRequest request) {
        Instant now = clock.instant();
        String raw = SecureTokens.generate();
        tokens.save(new RefreshToken(type, subjectId, SecureTokens.hash(raw), family, now,
                now.plus(properties.security().refreshTokenTtl()),
                request == null ? null : request.getHeader("User-Agent"),
                request == null ? null : request.getRemoteAddr()));
        return raw;
    }

    private static ApiException expired() {
        return ApiException.unauthorized("auth.session_expired", "Your session has ended. Sign in again.");
    }
}
