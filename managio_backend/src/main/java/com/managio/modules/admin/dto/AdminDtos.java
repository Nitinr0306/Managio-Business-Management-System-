package com.managio.modules.admin.dto;

import com.managio.platform.web.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * What the platform console shows (D-028). Tenants appear as metadata and counts only: no customer, invoice,
 * payment or amount of any business ever leaves through these types.
 */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** Days and weeks are counted in {@code timeZone}; "active" windows end at {@code generatedAt}. */
    public record PlatformOverview(Instant generatedAt, String timeZone, PlatformTotals totals, List<SignupWeek> growth,
                                   List<ActivityDay> activity, List<KeyCount> businessTypes, List<KeyCount> modules,
                                   List<FeatureUsage> features, ActivationFunnel activation) {
    }

    /**
     * A business is active when someone recorded something in it in the window; a person is active when they
     * signed in or used the app (a session started or refreshed).
     */
    public record PlatformTotals(long businesses, long newBusinesses30d, long activeBusinesses7d, long activeBusinesses30d,
                                 long archivedBusinesses, long users, long newUsers30d, long unverifiedUsers,
                                 long activeUsers1d, long activeUsers7d, long activeUsers30d, long teamSeats,
                                 long customers, long newCustomers30d, long portalCustomers, long activePortalCustomers30d) {
    }

    public record SignupWeek(LocalDate weekStart, long users, long businesses) {
    }

    public record ActivityDay(LocalDate day, long events, long activeBusinesses, long activeUsers) {
    }

    public record KeyCount(String key, long count) {
    }

    /** Use of one product area in the last 30 days: how many things were recorded, by how many businesses. */
    public record FeatureUsage(String area, long events, long businesses) {
    }

    /** How many open businesses have reached each step at least once. */
    public record ActivationFunnel(long created, long addedCustomer, long recordedPayment, long addedTeammate,
                                   long activeLast7Days) {
    }

    public record TenantRow(long id, String code, String name, String type, @Nullable String city, Instant createdAt,
                            @Nullable String ownerName, @Nullable String ownerEmail, long teamMembers, long customers,
                            @Nullable Instant lastActivityAt, boolean archived, List<String> modules) {
    }

    public record PlatformUserRow(long id, String fullName, String email, boolean emailVerified, boolean passwordSignIn,
                                  boolean googleSignIn, boolean disabled, boolean locked, long businesses,
                                  Instant createdAt, @Nullable Instant lastSignInAt, @Nullable Instant lastActiveAt) {
    }

    public record PlatformHealth(Instant checkedAt, AppStatus application, DatabaseStatus database,
                                 IntegrationStatus integrations, SignInStatus signIns, AccountStatus accounts) {
    }

    public record AppStatus(@Nullable String version, @Nullable Instant builtAt, String javaVersion, long uptimeSeconds,
                            long heapUsedBytes, long heapMaxBytes, int processors) {
    }

    public record DatabaseStatus(String serverVersion, long sizeBytes, @Nullable String schemaVersion,
                                 long migrationsApplied, boolean migrationsHealthy) {
    }

    /** Whether email is really sent (Resend) and Google sign-in is configured. */
    public record IntegrationStatus(boolean emailDelivery, boolean googleSignIn) {
    }

    public record SignInStatus(long succeeded24h, long failed24h, long lockedAccounts, long openSessions) {
    }

    public record AccountStatus(long unverifiedOver48h, long pendingInvitations, long expiredInvitations) {
    }
}
