package com.managio.modules.customers.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;

/** Someone a business serves: a member, client, student or patient depending on the business type. */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    public enum Status { ACTIVE, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private int number;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String guardianName;
    private String guardianPhone;
    private String notes;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private LocalDate joinedOn;
    private boolean portalEnabled;
    private String portalPasswordHash;
    private Instant portalEmailVerifiedAt;
    private int failedSignInCount;
    private Instant lockedUntil;
    private Instant lastSignInAt;
    private Long createdByUserId;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Customer create(long businessId, int number, Details details, LocalDate joinedOn, Long createdByUserId) {
        Customer customer = new Customer();
        customer.businessId = businessId;
        customer.number = number;
        customer.joinedOn = joinedOn;
        customer.createdByUserId = createdByUserId;
        customer.apply(details);
        return customer;
    }

    /** The editable profile fields. */
    public record Details(String fullName, String phone, String email, LocalDate dateOfBirth, String gender,
                          String address, String guardianName, String guardianPhone, String notes) {
    }

    public void apply(Details details) {
        this.fullName = details.fullName();
        this.phone = details.phone();
        this.email = details.email();
        this.dateOfBirth = details.dateOfBirth();
        this.gender = details.gender();
        this.address = details.address();
        this.guardianName = details.guardianName();
        this.guardianPhone = details.guardianPhone();
        this.notes = details.notes();
    }

    public boolean isArchived() {
        return status == Status.ARCHIVED;
    }

    public void archive() {
        this.status = Status.ARCHIVED;
        this.portalEnabled = false;
    }

    public void restore() {
        this.status = Status.ACTIVE;
    }

    public void setJoinedOn(LocalDate joinedOn) {
        this.joinedOn = joinedOn;
    }

    // ── Portal account ───────────────────────────────────────────────────────

    public boolean isPortalLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void enablePortal() {
        this.portalEnabled = true;
    }

    public void disablePortal() {
        this.portalEnabled = false;
    }

    public void setPortalPassword(String hash, Instant now) {
        this.portalPasswordHash = hash;
        this.failedSignInCount = 0;
        this.lockedUntil = null;
        if (email != null && portalEmailVerifiedAt == null) {
            this.portalEmailVerifiedAt = now;
        }
    }

    public void recordFailedPortalSignIn(Instant now, int maxAttempts, Duration lockFor) {
        failedSignInCount++;
        if (failedSignInCount >= maxAttempts) {
            lockedUntil = now.plus(lockFor);
            failedSignInCount = 0;
        }
    }

    public void recordPortalSignIn(Instant now) {
        failedSignInCount = 0;
        lockedUntil = null;
        lastSignInAt = now;
    }
}
