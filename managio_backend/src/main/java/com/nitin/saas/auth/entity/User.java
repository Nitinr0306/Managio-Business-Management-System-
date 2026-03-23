package com.nitin.saas.auth.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.common.utils.PublicIdGenerator;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email",   columnList = "email"),
        @Index(name = "idx_user_status",  columnList = "accountStatus"),
        @Index(name = "idx_user_created", columnList = "createdAt")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String publicId;

    @Column(nullable = false, unique = true, length = 255) private String email;
    @Column(nullable = false) private String password;
    @Column(length = 100) private String firstName;
    @Column(length = 100) private String lastName;
    @Column(length = 20)  private String phoneNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) @Column(name = "role")
    @Builder.Default private Set<Role> roles = new HashSet<>();

    /**
     * Defensive copy: callers may pass immutable sets (e.g., Set.of()).
     * Hibernate can mutate the collection during merge, so it must be mutable.
     */
    public void setRoles(Set<Role> roles) {
        if (roles == null) {
            this.roles = new HashSet<>();
            return;
        }
        this.roles = new HashSet<>(roles);
    }

    @Column(nullable = false) @Builder.Default private Boolean emailVerified = false;
    @Column(nullable = false) @Builder.Default private Boolean enabled       = true;
    @Column(nullable = false) @Builder.Default private Boolean accountLocked = false;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    @Builder.Default private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false) @Builder.Default private Integer failedLoginAttempts = 0;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lockedAt;
    private LocalDateTime passwordChangedAt;

    @Column(length = 500) private String profileImageUrl;
    @Column(length = 10)  private String preferredLanguage;
    @Column(length = 50)  private String timezone;
    @Column(nullable = false) @Builder.Default private Boolean twoFactorEnabled = false;
    @Column(length = 100) private String twoFactorSecret;
    @Column(columnDefinition = "TEXT") private String metadata;

    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp   @Column(nullable = false) private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    @Version private Long version;

    public enum AccountStatus { ACTIVE, SUSPENDED, DEACTIVATED, PENDING_VERIFICATION }

    @PrePersist
    protected void onCreate() {
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdGenerator.generate("OWN-", 8);
        }
        if (roles == null || roles.isEmpty()) { roles = new HashSet<>(); roles.add(Role.USER); }
    }

    public void incrementFailedAttempts() { failedLoginAttempts++; }
    public void resetFailedAttempts() { failedLoginAttempts = 0; lockedAt = null; }
    public void lockAccount() {
        accountLocked = true; accountStatus = AccountStatus.SUSPENDED;
        lockedAt = LocalDateTime.now();
    }
    public void unlockAccount() {
        accountLocked = false; accountStatus = AccountStatus.ACTIVE;
        failedLoginAttempts = 0; lockedAt = null;
    }
    public boolean isAccountNonExpired() { return accountStatus != AccountStatus.DEACTIVATED; }
    public boolean isAccountNonLocked()  { return !accountLocked; }
    public boolean isCredentialsNonExpired() {
        return passwordChangedAt == null
                || passwordChangedAt.plusDays(90).isAfter(LocalDateTime.now());
    }
    public String getFullName() {
        boolean hF = firstName != null && !firstName.isBlank();
        boolean hL = lastName  != null && !lastName.isBlank();
        if (hF && hL) return firstName.trim() + " " + lastName.trim();
        if (hF) return firstName.trim();
        if (hL) return lastName.trim();
        return (email != null && !email.isBlank()) ? email : "";
    }
}