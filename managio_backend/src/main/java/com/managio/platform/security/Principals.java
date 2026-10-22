package com.managio.platform.security;

/**
 * The two kinds of authenticated callers (D-006). They are separate types so that a customer ID can never be
 * used where a user ID is expected.
 */
public final class Principals {

    private Principals() {
    }

    /** An owner or team member, identified by their {@code users.id}. */
    public record UserPrincipal(long id, String email) {
    }

    /** A customer signed in to the portal of one business, identified by {@code customers.id}. */
    public record CustomerPrincipal(long id, long businessId) {
    }
}
