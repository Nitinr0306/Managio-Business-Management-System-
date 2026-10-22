package com.managio.modules.auth.domain;

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

/** An owner or team member account (D-006). */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    public enum Status { ACTIVE, DISABLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String passwordHash;
    private String fullName;
    private String phone;
    private Instant emailVerifiedAt;
    private String googleSubject;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private int failedSignInCount;
    private Instant lockedUntil;
    private Instant lastSignInAt;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static User withPassword(String email, String fullName, String passwordHash) {
        User user = new User();
        user.email = email;
        user.fullName = fullName;
        user.passwordHash = passwordHash;
        return user;
    }

    /** An account created through Google sign-in; Google has already verified the email. */
    public static User withGoogle(String email, String fullName, String googleSubject, Instant now) {
        User user = new User();
        user.email = email;
        user.fullName = fullName;
        user.googleSubject = googleSubject;
        user.emailVerifiedAt = now;
        return user;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    /** Counts a failed sign-in and locks the account once {@code maxAttempts} is reached (IDN-2). */
    public void recordFailedSignIn(Instant now, int maxAttempts, Duration lockFor) {
        failedSignInCount++;
        if (failedSignInCount >= maxAttempts) {
            lockedUntil = now.plus(lockFor);
            failedSignInCount = 0;
        }
    }

    public void recordSignIn(Instant now) {
        failedSignInCount = 0;
        lockedUntil = null;
        lastSignInAt = now;
    }

    public void setPassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.failedSignInCount = 0;
        this.lockedUntil = null;
    }

    /** Drops a password nobody proved they own the email for (see {@code AuthService.signInWithGoogle}). */
    public void removePassword() {
        this.passwordHash = null;
    }

    public void linkGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }
}
