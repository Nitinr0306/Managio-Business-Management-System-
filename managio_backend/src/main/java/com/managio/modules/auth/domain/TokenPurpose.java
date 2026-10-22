package com.managio.modules.auth.domain;

import java.time.Duration;

/** What a single-use emailed link is for, and how long it stays valid. */
public enum TokenPurpose {
    EMAIL_VERIFICATION(Duration.ofHours(48)),
    PASSWORD_RESET(Duration.ofHours(1)),
    PORTAL_INVITATION(Duration.ofDays(7));

    private final Duration lifetime;

    TokenPurpose(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public Duration lifetime() {
        return lifetime;
    }
}
