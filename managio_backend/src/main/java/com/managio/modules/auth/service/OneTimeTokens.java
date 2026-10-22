package com.managio.modules.auth.service;

import com.managio.modules.auth.domain.OneTimeToken;
import com.managio.modules.auth.domain.SubjectType;
import com.managio.modules.auth.domain.TokenPurpose;
import com.managio.modules.auth.repository.OneTimeTokenRepository;
import com.managio.platform.security.SecureTokens;
import com.managio.platform.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

/** Issues and redeems single-use links for email verification and password reset, for users and customers. */
@Service
@RequiredArgsConstructor
public class OneTimeTokens {

    private final OneTimeTokenRepository tokens;
    private final Clock clock;

    /** Creates a new token and retires any earlier unused token for the same purpose; returns the raw value. */
    @Transactional
    public String issue(TokenPurpose purpose, SubjectType type, long subjectId) {
        Instant now = clock.instant();
        tokens.retireUnused(purpose, type, subjectId, now);
        String raw = SecureTokens.generate();
        tokens.save(new OneTimeToken(purpose, type, subjectId, SecureTokens.hash(raw), now, now.plus(purpose.lifetime())));
        return raw;
    }

    /** Marks the token used and returns its subject ID; rejects unknown, used, expired or mismatched tokens. */
    @Transactional
    public long redeem(TokenPurpose purpose, SubjectType type, String rawToken) {
        return redeemAny(Set.of(purpose), type, rawToken);
    }

    /** Like {@link #redeem} for links that more than one purpose can produce, such as a portal invitation or reset. */
    @Transactional
    public long redeemAny(Set<TokenPurpose> purposes, SubjectType type, String rawToken) {
        Instant now = clock.instant();
        OneTimeToken token = tokens.findByTokenHash(SecureTokens.hash(rawToken))
                .filter(t -> purposes.contains(t.getPurpose()) && t.getSubjectType() == type)
                .orElseThrow(() -> invalidLink());
        if (!token.isUsable(now)) {
            throw invalidLink();
        }
        token.markUsed(now);
        return token.getSubjectId();
    }

    @Transactional
    public int deleteExpiredBefore(Instant cutoff) {
        return tokens.deleteExpiredBefore(cutoff);
    }

    private static ApiException invalidLink() {
        return ApiException.rule("token.invalid", "This link has expired or was already used. Request a new one.");
    }
}
