package com.nitin.saas.auth.entity;

import com.nitin.saas.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "accountStatus"),
        @Index(name = "idx_user_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lockedAt;

    private LocalDateTime passwordChangedAt;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(length = 10)
    private String preferredLanguage;

    @Column(length = 50)
    private String timezone;

    @Column(nullable = false)
    @Builder.Default
    private Boolean twoFactorEnabled = false;

    @Column(length = 100)
    private String twoFactorSecret;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public enum AccountStatus {
        ACTIVE,
        SUSPENDED,
        DEACTIVATED,
        PENDING_VERIFICATION
    }

    @PrePersist
    protected void onCreate() {
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add(Role.USER);
        }
    }

    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
    }

    public void lockAccount() {
        this.accountLocked = true;
        this.accountStatus = AccountStatus.SUSPENDED;
        this.lockedAt = LocalDateTime.now();
    }

    public void unlockAccount() {
        this.accountLocked = false;
        this.accountStatus = AccountStatus.ACTIVE;
        this.failedLoginAttempts = 0;
        this.lockedAt = null;
    }

    public boolean isAccountNonExpired() {
        return accountStatus != AccountStatus.DEACTIVATED;
    }

    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    public boolean isCredentialsNonExpired() {
        if (passwordChangedAt == null) {
            return true;
        }
        return passwordChangedAt.plusDays(90).isAfter(LocalDateTime.now());
    }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}